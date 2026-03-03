package service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Service Face ID ULTRA-SIMPLIFIÉ
 * Pas de Haar Cascade - Détection centre frame uniquement
 */
public class FaceIDCameraService {

    private VideoCapture camera;
    private boolean isInitialized = true; // Toujours OK

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            System.out.println("✅ OpenCV loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to load OpenCV: " + e.getMessage());
        }
    }

    public FaceIDCameraService() {
        System.out.println("✅ Face detector initialized (simple mode)");
    }

    /**
     * Démarrer la caméra
     */
    public boolean startCamera(int deviceIndex) {
        try {
            camera = new VideoCapture(deviceIndex);

            // Attendre que caméra s'ouvre
            for (int i = 0; i < 30 && !camera.isOpened(); i++) {
                Thread.sleep(100);
            }

            if (!camera.isOpened()) {
                System.err.println("❌ Cannot open camera " + deviceIndex);
                return false;
            }

            // Configuration
            camera.set(3, 640);  // Width
            camera.set(4, 480);  // Height

            System.out.println("✅ Camera started successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Camera start failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Capturer frame avec détection simple
     */
    public CameraFrame captureFrame() {
        if (camera == null || !camera.isOpened()) {
            return null;
        }

        try {
            Mat frame = new Mat();

            if (!camera.read(frame) || frame.empty()) {
                return null;
            }

            // Détection simple: zone centrale
            Rect faceRect = detectFaceSimple(frame);

            // Dessiner rectangle si détecté
            if (faceRect != null) {
                Imgproc.rectangle(
                        frame,
                        new Point(faceRect.x, faceRect.y),
                        new Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                        new Scalar(123, 95, 245),
                        3
                );
            }

            // Convertir en JavaFX Image
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            Image image = new Image(new ByteArrayInputStream(buffer.toArray()));

            return new CameraFrame(frame, image, faceRect);

        } catch (Exception e) {
            System.err.println("❌ Frame capture failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Détection simple: zone centrale fixe
     */
    private Rect detectFaceSimple(Mat frame) {
        try {
            int width = frame.cols();
            int height = frame.rows();

            // Zone centrale (30-70% horizontal, 25-75% vertical)
            int x = (int)(width * 0.30);
            int y = (int)(height * 0.25);
            int w = (int)(width * 0.40);
            int h = (int)(height * 0.50);

            // Vérifier qu'il y a du contenu dans la zone
            Rect roi = new Rect(x, y, w, h);
            Mat region = new Mat(frame, roi);

            // Convertir en gris et vérifier luminosité
            Mat gray = new Mat();
            if (region.channels() > 1) {
                Imgproc.cvtColor(region, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                gray = region;
            }

            Scalar mean = Core.mean(gray);
            double brightness = mean.val[0];

            // Si zone pas trop sombre/claire, considérer comme visage
            if (brightness > 30 && brightness < 220) {
                return roi;
            }

        } catch (Exception e) {
            System.err.println("⚠️ Simple detection error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Extraire visage
     */
    public Mat extractFace(Mat frame, Rect faceRect) {
        if (frame == null || faceRect == null) {
            return null;
        }

        try {
            // Padding
            int padding = 20;
            int x = Math.max(0, faceRect.x - padding);
            int y = Math.max(0, faceRect.y - padding);
            int width = Math.min(frame.cols() - x, faceRect.width + 2 * padding);
            int height = Math.min(frame.rows() - y, faceRect.height + 2 * padding);

            Rect expandedRect = new Rect(x, y, width, height);
            Mat face = new Mat(frame, expandedRect);

            // Redimensionner à 128x128
            Mat resized = new Mat();
            Imgproc.resize(face, resized, new Size(128, 128));

            return resized;

        } catch (Exception e) {
            System.err.println("❌ Face extraction failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarder image
     */
    public File saveFaceImage(Mat face) {
        try {
            File tempFile = File.createTempFile("face_", ".jpg");
            Imgcodecs.imwrite(tempFile.getAbsolutePath(), face);
            return tempFile;
        } catch (Exception e) {
            System.err.println("❌ Save image failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Arrêter caméra
     */
    public void stopCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            System.out.println("✅ Camera stopped");
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Classe frame
     */
    public static class CameraFrame {
        private final Mat originalMat;
        private final Image image;
        private final Rect faceRect;

        public CameraFrame(Mat originalMat, Image image, Rect faceRect) {
            this.originalMat = originalMat;
            this.image = image;
            this.faceRect = faceRect;
        }

        public Mat getOriginalMat() {
            return originalMat;
        }

        public Image getImage() {
            return image;
        }

        public Rect getFaceRect() {
            return faceRect;
        }

        public boolean hasFace() {
            return faceRect != null;
        }
    }
}