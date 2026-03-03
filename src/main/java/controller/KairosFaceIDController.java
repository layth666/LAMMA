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
import service.*;
import utils.Session;

import java.io.File;

/**
 * Face ID Dialog avec Kairos API
 * Reconnaissance faciale professionnelle et précise
 */
public class KairosFaceIDController {

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
    private KairosService kairosService;
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
    private String recognizedSubjectId = null;
    private double recognizedConfidence = 0.0;

    private static final int SCAN_TIME = 5;

    public void initDialog(Stage parent) {
        this.parentStage = parent;
        this.cameraService = new FaceIDCameraService();
        this.kairosService = new KairosService();
        this.cloudinaryService = new CloudinaryService();
        this.userService = new UserService();
    }

    @FXML
    public void initialize() {
        System.out.println("✅ Kairos Face ID Controller initialized");
        System.out.println("🔍 Buttons: Try=" + (tryAgainBtn != null) + ", Register=" + (registerBtn != null));

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

                    // Capturer image toutes les 2 secondes
                    if (frame.hasFace() && (now - lastCapture > 2_000_000_000)) {
                        lastCapture = now;
                        captureAndRecognize(frame);
                    } else if (!frame.hasFace()) {
                        matchLabel.setText("---%");
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

            if (scanTimeRemaining <= 0) handleScanTimeout();
        }));
        scanTimer.setCycleCount(SCAN_TIME);
        scanTimer.play();
    }

    private void captureAndRecognize(FaceIDCameraService.CameraFrame frame) {
        new Thread(() -> {
            try {
                System.out.println("📸 Capturing face for Kairos recognition...");

                // Extraire et sauvegarder l'image
                org.opencv.core.Mat faceMat = cameraService.extractFace(
                        frame.getOriginalMat(),
                        frame.getFaceRect()
                );

                if (faceMat == null) {
                    System.out.println("⚠️ Face extraction failed");
                    return;
                }

                File imageFile = cameraService.saveFaceImage(faceMat);
                if (imageFile == null) {
                    System.out.println("⚠️ Failed to save image");
                    return;
                }

                capturedImageFile = imageFile;

                System.out.println("🔍 Calling Kairos recognize API...");
                Platform.runLater(() -> updateStatus("🔍 Recognizing with Kairos..."));

                // Appeler Kairos API
                KairosService.KairosResponse response = kairosService.recognizeFace(imageFile);

                System.out.println("📊 Kairos response: " + response);

                if (response.success && response.subjectId != null) {
                    recognizedSubjectId = response.subjectId;
                    recognizedConfidence = response.confidence;

                    Platform.runLater(() -> {
                        matchLabel.setText(String.format("%.0f%%", response.confidence));
                        matchLabel.setStyle("-fx-text-fill: #10b981;");
                    });

                    // Si confiance élevée, login automatique
                    if (response.confidence >= 70.0) {
                        Platform.runLater(() -> handleMatchFound());
                    }
                } else {
                    Platform.runLater(() -> {
                        matchLabel.setText("0%");
                        matchLabel.setStyle("-fx-text-fill: #ef4444;");
                    });
                }

            } catch (Exception e) {
                System.err.println("❌ Recognition error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMatchFound() {
        if (scanComplete) return;

        scanComplete = true;
        if (scanTimer != null) scanTimer.stop();
        if (cameraTimer != null) cameraTimer.stop();

        updateStatus("✅ Face recognized!");

        System.out.println("═══════════════════════════════════════════");
        System.out.println("✅ MATCH FOUND via Kairos");
        System.out.println("👤 Subject ID: " + recognizedSubjectId);
        System.out.println("📊 Confidence: " + recognizedConfidence + "%");
        System.out.println("═══════════════════════════════════════════");

        new Thread(() -> {
            try {
                // Convertir subject_id en user_id
                // Format: "user_123" -> 123
                int userId = Integer.parseInt(recognizedSubjectId.replace("user_", ""));

                User user = userService.recuperer().stream()
                        .filter(u -> u.getId() == userId)
                        .findFirst().orElse(null);

                if (user != null) {
                    System.out.println("✅ User found: " + user.getName());
                    Session.getInstance().setCurrentUser(user);
                    Platform.runLater(() -> showSuccess(user));
                } else {
                    System.err.println("❌ User not found for ID: " + userId);
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

        System.out.println("⏱️ Scan timeout - No match found");

        cameraService.stopCamera();
        cameraPane.setVisible(false);
        resultPanel.setVisible(true);

        String message;
        if (recognizedSubjectId == null) {
            message = "Face not recognized\nWould you like to register?";
        } else {
            message = "Low confidence match: " + String.format("%.0f%%", recognizedConfidence) + "\nNeed ≥70% for auto-login";
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
        resultMessage.setText(user.getName() + "\n\nConfidence: " + String.format("%.0f%%", recognizedConfidence));

        tryAgainBtn.setVisible(false);
        registerBtn.setVisible(false);

        Timeline autoLogin = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                navigateToDashboard(user);
            } else {
                close();
                showInfo("Success", "Welcome, " + user.getName() + "!");
            }
        }));
        autoLogin.play();
    }

    @FXML
    private void handleTryAgain() {
        System.out.println("🔄 Try Again clicked");

        scanComplete = false;
        scanTimeRemaining = SCAN_TIME;
        recognizedSubjectId = null;
        recognizedConfidence = 0.0;
        capturedImageFile = null;

        resultPanel.setVisible(false);
        cameraPane.setVisible(true);
        matchLabel.setText("---%");
        timerLabel.setText(SCAN_TIME + "s");

        startScan();
    }

    @FXML
    private void handleRegister() {
        System.out.println("📝 Register clicked");

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Register Face with Kairos");
        dialog.setHeaderText("Enter your email address");
        dialog.setContentText("Email:");

        dialog.showAndWait().ifPresent(email -> {
            System.out.println("📧 Email entered: " + email);
            enrollFaceWithKairos(email);
        });
    }

    private void enrollFaceWithKairos(String email) {
        new Thread(() -> {
            try {
                System.out.println("🔍 Finding user: " + email);
                User user = userService.findByEmail(email);

                if (user == null) {
                    Platform.runLater(() -> showError("Error", "No user found with this email"));
                    return;
                }

                String subjectId = "user_" + user.getId();

                // Vérifier si déjà enregistré
                if (kairosService.subjectExists(subjectId)) {
                    Platform.runLater(() -> showError("Error", "This user already has a face registered in Kairos"));
                    return;
                }

                if (capturedImageFile == null) {
                    Platform.runLater(() -> showError("Error", "No image captured. Please try again."));
                    return;
                }

                Platform.runLater(() -> updateStatus("📤 Enrolling face with Kairos..."));

                System.out.println("📤 Enrolling to Kairos with subject_id: " + subjectId);
                KairosService.KairosResponse response = kairosService.enrollFace(capturedImageFile, subjectId);

                System.out.println("📊 Kairos enrollment response: " + response);

                if (response.success) {
                    // Upload image to Cloudinary aussi
                    String imageUrl = cloudinaryService.uploadImage(capturedImageFile);
                    System.out.println("📤 Image uploaded to Cloudinary: " + imageUrl);

                    Session.getInstance().setCurrentUser(user);
                    Platform.runLater(() -> showEnrollmentSuccess(user));
                } else {
                    Platform.runLater(() -> showError("Error", "Kairos enrollment failed: " + response.message));
                }

            } catch (Exception e) {
                System.err.println("❌ Enrollment error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showError("Error", "Enrollment failed: " + e.getMessage()));
            } finally {
                // Cleanup temp file
                if (capturedImageFile != null && capturedImageFile.exists()) {
                    capturedImageFile.delete();
                }
            }
        }).start();
    }

    private void showEnrollmentSuccess(User user) {
        resultIcon.setText("✅");
        resultTitle.setText("Face Registered!");
        resultTitle.setStyle("-fx-text-fill: #10b981;");
        resultMessage.setText("Welcome, " + user.getName() + "!\n\nYour face is now registered with Kairos\nYou can use Face ID to login");

        tryAgainBtn.setVisible(false);
        registerBtn.setVisible(false);

        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                navigateToDashboard(user);
            } else {
                close();
            }
        }));
        autoClose.play();
    }

    private void navigateToDashboard(User user) {
        try {
            close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setCurrentUser(user);

            parentStage.close();
            Stage dashboardStage = new Stage();
            dashboardStage.initStyle(StageStyle.UNDECORATED);
            dashboardStage.setScene(new Scene(root));
            dashboardStage.setMaximized(true);
            dashboardStage.show();
        } catch (Exception e) {
            showError("Error", "Failed to open dashboard");
        }
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
            FXMLLoader loader = new FXMLLoader(KairosFaceIDController.class.getResource("/FaceIDDialog.fxml"));
            Parent root = loader.load();

            KairosFaceIDController controller = loader.getController();

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