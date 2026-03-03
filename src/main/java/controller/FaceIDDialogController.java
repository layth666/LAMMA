package controller;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.User;
import org.opencv.core.Mat;
import service.*;
import utils.Session;
import utils.NetworkUtils;

import java.io.File;

/**
 * FaceIDDialogController avec Face++ API
 * Utilise vos clés Face++ déjà configurées
 */
public class FaceIDDialogController {

    @FXML private Label timerLabel;
    @FXML private StackPane cameraPane;
    @FXML private ImageView cameraView;
    @FXML private Label matchLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    @FXML private VBox resultPanel;
    @FXML private Label resultIcon;
    @FXML private Label resultTitle;
    @FXML private Label resultMessage;
    @FXML private Button tryAgainBtn;
    @FXML private Button registerBtn;

    // Services
    private FaceIDCameraService cameraService;
    private FacePlusPlusService facePlusPlusService;
    private CloudinaryService cloudinaryService;
    private UserService userService;

    // State
    private Stage dialogStage;
    private Stage parentStage;
    private AnimationTimer cameraTimer;
    private Timeline scanTimer;
    private boolean scanComplete = false;
    private int scanTimeRemaining = 5;
    private File capturedImageFile = null;
    private double bestConfidence = 0.0;
    private int matchedUserId = -1;

    private static final int SCAN_TIME = 5;
    private static final double CONFIDENCE_THRESHOLD = 70.0; // Face++ threshold

    public void initDialog(Stage parent) {
        this.parentStage = parent;
        this.cameraService = new FaceIDCameraService();
        this.facePlusPlusService = new FacePlusPlusService();
        this.cloudinaryService = new CloudinaryService();
        this.userService = new UserService();

        System.out.println("✅ Services initialized with Face++");
    }

    @FXML
    public void initialize() {
        System.out.println("✅ FaceIDDialogController initialized (Face++ version)");
        System.out.println("🔍 Try Again Button: " + (tryAgainBtn != null ? "EXISTS" : "NULL"));
        System.out.println("🔍 Register Button: " + (registerBtn != null ? "EXISTS" : "NULL"));

        // Hover effects
        if (tryAgainBtn != null) {
            tryAgainBtn.setOnMouseEntered(e ->
                    tryAgainBtn.setStyle("-fx-background-color: #f0ebff; -fx-text-fill: #7B5FF5; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 8; -fx-border-color: #7B5FF5; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;")
            );
            tryAgainBtn.setOnMouseExited(e ->
                    tryAgainBtn.setStyle("-fx-background-color: white; -fx-text-fill: #7B5FF5; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 8; -fx-border-color: #7B5FF5; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;")
            );
        }

        if (registerBtn != null) {
            registerBtn.setOnMouseEntered(e ->
                    registerBtn.setStyle("-fx-background-color: #6B4FE5; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;")
            );
            registerBtn.setOnMouseExited(e ->
                    registerBtn.setStyle("-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;")
            );
        }
    }

    public void startScan() {
        // Vérifier connexion internet
        if (!NetworkUtils.isInternetConnected()) {
            showError("No Internet", "Face++ requires an internet connection.\nPlease check your network and try again.");
            close();
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> updateStatus("Starting camera..."));

                if (!cameraService.startCamera(0)) {
                    Platform.runLater(() -> {
                        showError("Camera Error", "Could not access camera");
                        close();
                    });
                    return;
                }

                Platform.runLater(() -> {
                    updateStatus("✅ Look at the camera...");
                    startCameraLoop();
                    startCountdownTimer();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void startCameraLoop() {
        cameraTimer = new AnimationTimer() {
            private long lastCapture = 0;

            @Override
            public void handle(long now) {
                if (scanComplete) return;

                FaceIDCameraService.CameraFrame frame = cameraService.captureFrame();
                if (frame != null && frame.getImage() != null) {
                    cameraView.setImage(frame.getImage());

                    // Capturer toutes les 2 secondes
                    if (frame.hasFace() && (now - lastCapture > 2_000_000_000)) {
                        lastCapture = now;
                        captureForComparison(frame);
                    } else if (!frame.hasFace()) {
                        matchLabel.setText("---%");
                        matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: white;");
                    }
                }
            }
        };
        cameraTimer.start();
    }

    private void startCountdownTimer() {
        scanTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            scanTimeRemaining--;
            timerLabel.setText(scanTimeRemaining + "s");
            progressBar.setProgress((double) scanTimeRemaining / SCAN_TIME);

            if (scanTimeRemaining <= 0) {
                handleScanTimeout();
            }
        }));
        scanTimer.setCycleCount(SCAN_TIME);
        scanTimer.play();
    }

    private void captureForComparison(FaceIDCameraService.CameraFrame frame) {
        new Thread(() -> {
            try {
                System.out.println("📸 Capturing face for Face++ comparison...");

                Mat faceMat = cameraService.extractFace(frame.getOriginalMat(), frame.getFaceRect());
                if (faceMat == null) {
                    System.out.println("⚠️ Face extraction failed");
                    return;
                }

                File tempImageFile = cameraService.saveFaceImage(faceMat);
                if (tempImageFile == null) {
                    System.out.println("⚠️ Failed to save image");
                    return;
                }

                capturedImageFile = tempImageFile;

                Platform.runLater(() -> updateStatus("🔍 Comparing with Face++..."));

                // Comparer avec toutes les images enregistrées
                compareWithAllUsers(tempImageFile);

            } catch (Exception e) {
                System.err.println("❌ Capture error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void compareWithAllUsers(File capturedImage) {
        try {
            // Récupérer tous les users
            java.util.List<User> allUsers = userService.recuperer();

            if (allUsers.isEmpty()) {
                System.out.println("⚠️ No users in database");
                Platform.runLater(() -> {
                    matchLabel.setText("0%");
                    matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                });
                return;
            }

            System.out.println("📊 Comparing with " + allUsers.size() + " registered users...");

            double maxConfidence = 0.0;
            int bestMatchUserId = -1;

            // Dossier où les photos Face ID sont stockées
            File facesDir = new File("src/main/resources/faces");
            if (!facesDir.exists()) {
                System.out.println("⚠️ Faces directory doesn't exist yet");
                Platform.runLater(() -> {
                    matchLabel.setText("0%");
                    matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                });
                return;
            }

            for (User user : allUsers) {
                // Chercher le fichier face de ce user
                String faceFileName = user.getEmail().replaceAll("[^a-zA-Z0-9.-]", "_") + "_face.jpg";
                File userFaceFile = new File(facesDir, faceFileName);

                if (!userFaceFile.exists()) {
                    System.out.println("  ⏭️ User " + user.getName() + " has no face registered");
                    continue;
                }

                System.out.println("  📊 Comparing with " + user.getName() + "...");

                // Comparer avec Face++
                double confidence = facePlusPlusService.compareFaces(capturedImage, userFaceFile);

                if (confidence > 0) {
                    System.out.println("     Confidence: " + confidence + "%");

                    if (confidence > maxConfidence) {
                        maxConfidence = confidence;
                        bestMatchUserId = user.getId();
                    }
                } else if (confidence == -2.0) {
                    System.out.println("     ⚠️ No face in captured image");
                } else if (confidence == -3.0) {
                    System.out.println("     ⚠️ No face in stored image");
                } else {
                    System.out.println("     ❌ Comparison failed");
                }
            }

            final double finalConfidence = maxConfidence;
            final int finalUserId = bestMatchUserId;

            Platform.runLater(() -> {
                matchLabel.setText(String.format("%.0f%%", finalConfidence));

                if (finalConfidence >= CONFIDENCE_THRESHOLD) {
                    matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: #10b981;");

                    bestConfidence = finalConfidence;
                    matchedUserId = finalUserId;

                    handleMatchFound();
                } else if (finalConfidence > 50) {
                    matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: #f59e0b;");
                } else {
                    matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Error comparing with users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMatchFound() {
        if (scanComplete) return;

        scanComplete = true;
        if (scanTimer != null) scanTimer.stop();
        if (cameraTimer != null) cameraTimer.stop();

        updateStatus("✅ Face recognized!");

        System.out.println("═══════════════════════════════════════════");
        System.out.println("✅ MATCH FOUND via Face++");
        System.out.println("👤 User ID: " + matchedUserId);
        System.out.println("📊 Confidence: " + String.format("%.1f%%", bestConfidence));
        System.out.println("═══════════════════════════════════════════");

        new Thread(() -> {
            try {
                User user = userService.recuperer().stream()
                        .filter(u -> u.getId() == matchedUserId)
                        .findFirst().orElse(null);

                if (user != null) {
                    System.out.println("✅ User found: " + user.getName());
                    Session.getInstance().setCurrentUser(user);
                    Platform.runLater(() -> showSuccess(user));
                } else {
                    System.err.println("❌ User not found for ID: " + matchedUserId);
                    Platform.runLater(this::handleScanTimeout);
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(this::handleScanTimeout);
            }
        }).start();
    }

    private void handleScanTimeout() {
        if (scanComplete) return;

        scanComplete = true;
        if (scanTimer != null) scanTimer.stop();
        if (cameraTimer != null) cameraTimer.stop();

        System.out.println("⏱️ Scan timeout - Best confidence: " + bestConfidence + "%");

        cameraService.stopCamera();
        cameraPane.setVisible(false);
        resultPanel.setVisible(true);

        String message;
        if (matchedUserId == -1) {
            message = "Face not recognized\nWould you like to register?";
        } else {
            message = "Low confidence: " + String.format("%.0f%%", bestConfidence) + "\nNeed ≥" + (int)CONFIDENCE_THRESHOLD + "% for auto-login";
        }

        resultMessage.setText(message);
    }

    private void showSuccess(User user) {
        cameraService.stopCamera();
        cameraPane.setVisible(false);
        resultPanel.setVisible(true);

        resultIcon.setText("✅");
        resultTitle.setText("Welcome back!");
        resultTitle.setStyle("-fx-text-fill: #10b981;");
        resultMessage.setText(user.getName() +
                "\n\nFace++ Confidence: " +
                String.format("%.0f%%", bestConfidence));

        tryAgainBtn.setVisible(false);
        registerBtn.setVisible(false);

        Timeline autoLogin = new Timeline(
                new KeyFrame(Duration.seconds(1.5), e -> {
                    close(); // ONLY close dialog
                })
        );
        autoLogin.play();
    }

    @FXML
    private void handleTryAgain() {
        System.out.println("🔄 Try Again clicked");

        scanComplete = false;
        scanTimeRemaining = SCAN_TIME;
        bestConfidence = 0.0;
        matchedUserId = -1;
        capturedImageFile = null;

        resultPanel.setVisible(false);
        cameraPane.setVisible(true);
        matchLabel.setText("---%");
        matchLabel.setStyle("-fx-font-size: 40; -fx-font-weight: 700; -fx-text-fill: white;");
        timerLabel.setText(SCAN_TIME + "s");

        startScan();
    }

    @FXML
    private void handleRegister() {
        System.out.println("📝 Register clicked");

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Register Face with Face++");
        dialog.setHeaderText("Enter your email address");
        dialog.setContentText("Email:");

        dialog.showAndWait().ifPresent(email -> {
            System.out.println("📧 Email entered: " + email);
            enrollFace(email);
        });
    }

    private void enrollFace(String email) {
        new Thread(() -> {
            try {
                System.out.println("🔍 Finding user: " + email);
                User user = userService.findByEmail(email);

                if (user == null) {
                    Platform.runLater(() -> showError("Error", "No user found with this email"));
                    return;
                }

                System.out.println("✅ User found: " + user.getName() + " (ID: " + user.getId() + ")");

                // Vérifier si a déjà une face
                String faceFileName = email.replaceAll("[^a-zA-Z0-9.-]", "_") + "_face.jpg";
                File facesDir = new File("src/main/resources/faces");
                File existingFaceFile = new File(facesDir, faceFileName);

                if (existingFaceFile.exists()) {
                    Platform.runLater(() -> showError("Error", "This user already has a face registered"));
                    return;
                }

                if (capturedImageFile == null) {
                    Platform.runLater(() -> showError("Error", "No image captured. Please try again."));
                    return;
                }

                Platform.runLater(() -> updateStatus("💾 Saving face..."));

                // Créer dossier si nécessaire
                if (!facesDir.exists()) {
                    facesDir.mkdirs();
                    System.out.println("✅ Created faces directory: " + facesDir.getAbsolutePath());
                }

                // Copier le fichier capturé vers le dossier faces
                File savedFaceFile = new File(facesDir, faceFileName);
                java.nio.file.Files.copy(
                        capturedImageFile.toPath(),
                        savedFaceFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                System.out.println("✅ Face image saved: " + savedFaceFile.getAbsolutePath());

                // Upload vers Cloudinary aussi (optionnel)
                try {
                    String imageUrl = cloudinaryService.uploadImage(savedFaceFile);
                    System.out.println("📤 Image uploaded to Cloudinary: " + imageUrl);
                } catch (Exception e) {
                    System.out.println("⚠️ Cloudinary upload failed (non-critical): " + e.getMessage());
                }

                Session.getInstance().setCurrentUser(user);
                Platform.runLater(() -> showEnrollmentSuccess(user));

            } catch (Exception e) {
                System.err.println("❌ Enrollment error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showError("Error", "Enrollment failed: " + e.getMessage()));
            } finally {
                if (capturedImageFile != null && capturedImageFile.exists()) {
                    capturedImageFile.delete();
                }
            }
        }).start();
    }

    private void showEnrollmentSuccess(User user) {

        cameraService.stopCamera();
        cameraPane.setVisible(false);
        resultPanel.setVisible(true);

        resultIcon.setText("✅");
        resultTitle.setText("Face Registered!");
        resultTitle.setStyle("-fx-text-fill: #10b981;");
        resultMessage.setText("Welcome, " + user.getName() +
                "!\n\nYour face is now registered.\nYou can now use Face ID to login.");

        tryAgainBtn.setVisible(false);
        registerBtn.setVisible(false);

        // IMPORTANT: set session
        Session.getInstance().setCurrentUser(user);

        Timeline autoClose = new Timeline(
                new KeyFrame(Duration.seconds(2.5), e -> {
                    close(); // ONLY close dialog
                })
        );

        autoClose.play();
    }



    @FXML
    private void handleCancel() {
        close();
    }

    private void updateStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    public void setStage(Stage stage) {
        this.dialogStage = stage;
    }

    private void close() {
        if (cameraTimer != null) cameraTimer.stop();
        if (scanTimer != null) scanTimer.stop();
        cameraService.stopCamera();
        if (capturedImageFile != null && capturedImageFile.exists()) {
            capturedImageFile.delete();
        }
        if (dialogStage != null) dialogStage.close();
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(FaceIDDialogController.class.getResource("/FaceIDDialog.fxml"));
            Parent root = loader.load();

            FaceIDDialogController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setScene(new Scene(root));

            controller.setStage(dialog);
            controller.initDialog(owner);

            dialog.setOnShown(e -> controller.startScan());

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Failed to open Face ID dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }
}