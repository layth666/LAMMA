package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.User;
import service.GoogleAuthService;
import service.LoginAttemptService;
import service.LoginAttemptService.LoginStatus;
import service.UserService;
import utils.PasswordHasher;
import utils.Session;

import java.sql.SQLException;

public class Logincontroller {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button loginBtn;
    @FXML private Button googleLoginBtn;
    @FXML private Button togglePasswordBtn;
    @FXML private Label eyeIcon;
    @FXML private Hyperlink signupLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button closeBtn;
    @FXML private Button minimizeBtn;
    @FXML private Button maximizeBtn;

    private UserService userService = new UserService();
    private LoginAttemptService attemptService = LoginAttemptService.getInstance();


    @FXML
    public void initialize() {
        passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());
        setupButtonHoverEffects();
    }

    private void setupButtonHoverEffects() {
        // Login Button - Orange glow
        loginBtn.setOnMouseEntered(e -> {
            if (!loginBtn.isDisabled()) {
                loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #FF9A56 0%, #FF7A45 100%); -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: 700; -fx-background-radius: 27; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 53, 0.6), 20, 0, 0, 7); -fx-letter-spacing: 1;");
            }
        });

        loginBtn.setOnMouseExited(e -> {
            if (!loginBtn.isDisabled()) {
                loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #FF8C42 0%, #FF6B35 100%); -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: 700; -fx-background-radius: 27; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 53, 0.4), 15, 0, 0, 5); -fx-letter-spacing: 1;");
            }
        });

        googleLoginBtn.setOnMouseEntered(e -> {
            googleLoginBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: 600; -fx-border-color: rgba(255,255,255,0.4); -fx-border-width: 1.5; -fx-border-radius: 23; -fx-background-radius: 23; -fx-cursor: hand;");
        });

        googleLoginBtn.setOnMouseExited(e -> {
            googleLoginBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13; -fx-font-weight: 600; -fx-border-color: rgba(255,255,255,0.25); -fx-border-width: 1.5; -fx-border-radius: 23; -fx-background-radius: 23; -fx-cursor: hand;");
        });
    }

    @FXML
    private void handleGoogleLogin() {
        System.out.println("🔑 Opening Google authentication window...");

        try {
            Stage currentStage = (Stage) googleLoginBtn.getScene().getWindow();
            GoogleAuthDialog authDialog = new GoogleAuthDialog(currentStage);

            authDialog.setOnSuccess(googleUser -> {
                System.out.println("✅ Google auth successful!");
                try {
                    handleGoogleAuthSuccess(googleUser);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Database error: " + e.getMessage());
                }
            });

            authDialog.setOnError(error -> {
                System.err.println("❌ Google auth error: " + error.getMessage());
                if (!error.getMessage().contains("cancelled")) {
                    showAlert("Google authentication failed: " + error.getMessage());
                }
            });

            authDialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error opening Google sign-in: " + e.getMessage());
        }
    }

    private void handleGoogleAuthSuccess(GoogleAuthService.GoogleUserInfo googleUser) throws SQLException {
        System.out.println("✅ Processing Google user: " + googleUser.getEmail());

        User existingUser = userService.findByEmail(googleUser.getEmail());

        if (existingUser != null) {
            // Reset attempts pour Google login
            attemptService.resetAttempts(googleUser.getEmail());

            Session.getInstance().setCurrentUser(existingUser);
            showSuccess("Welcome back, " + existingUser.getName() + "! 🎉");

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            delay.setOnFinished(event -> {
                try {
                    if ("ADMIN".equalsIgnoreCase(existingUser.getRole())) {
                        loadDashboard(existingUser);
                    } else {
                        loadMainApp(existingUser);
                    }
                } catch (Exception e) {
                    showAlert("Navigation error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            delay.play();

        } else {
            String randomPassword = PasswordHasher.hashPassword(java.util.UUID.randomUUID().toString());

            User newUser = new User(
                    googleUser.getName(),
                    googleUser.getEmail(),
                    randomPassword,
                    "USER",
                    "Not provided",
                    "NO",
                    googleUser.getPicture()
            );

            userService.ajouter(newUser);
            User createdUser = userService.findByEmail(googleUser.getEmail());

            Session.getInstance().setCurrentUser(createdUser);
            showSuccess("Welcome to LAMMA, " + createdUser.getName() + "! 🎉");

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            delay.setOnFinished(event -> {
                try {
                    loadMainApp(createdUser);
                } catch (Exception e) {
                    showAlert("Navigation error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            delay.play();
        }
    }

    /**
     * ✅ LOGIN WITH ATTEMPT TRACKING AND BAN SYSTEM
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String plainPassword = passwordField.getText();

        if (email.isEmpty() || plainPassword.isEmpty()) {
            showAlert("Please enter email and password");
            return;
        }

        // Check if this email is allowed to login
        LoginStatus status = attemptService.checkLoginAllowed(email);

        if (!status.allowed) {
            if ("BANNED".equals(status.status)) {
                showBanNotification(status, email); // only disable button for this email
                return;
            } else if ("COOLDOWN".equals(status.status)) {
                showCooldownNotification(status, email); // only disable button for this email
                return;
            }
        }

        try {
            User user = userService.login(email, plainPassword);

            if (user != null) {
                // ✅ SUCCESS - Reset attempts for this email
                attemptService.resetAttempts(email);

                Session.getInstance().setCurrentUser(user);
                showSuccess("Welcome back, " + user.getName() + "! 🎉");

                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                delay.setOnFinished(event -> {
                    try {
                        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            loadDashboard(user);
                        } else {
                            loadMainApp(user);
                        }
                    } catch (Exception e) {
                        showAlert("Navigation error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                delay.play();

                clearFields();

            } else {
                // ❌ FAILED - Record attempt for this email
                LoginStatus failStatus = attemptService.recordFailedAttempt(email);

                if ("COOLDOWN".equals(failStatus.status)) {
                    showCooldownNotification(failStatus, email);
                } else if ("BANNED".equals(failStatus.status)) {
                    showBanNotification(failStatus, email);
                } else {
                    showAlert("Invalid email or password");
                }
            }

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Afficher notification de cooldown (30s)
     */
    private void showCooldownNotification(LoginStatus status, String email) {
        // Only update button if typed email matches
        if (!emailField.getText().trim().equals(email)) return;

        loginBtn.setDisable(true);

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    LoginStatus current = attemptService.checkLoginAllowed(email);
                    if (current.allowed) {
                        loginBtn.setDisable(false);
                        loginBtn.setText("Sign In");
                        loginBtn.setStyle("-fx-background-color: #7B5FF5; -fx-text-fill: white;");
                    } else {
                        loginBtn.setText("Locked (" + current.getFormattedTime() + ")");
                    }
                })
        );

        timeline.setCycleCount((int) status.secondsRemaining + 1);
        timeline.play();
    }

    /**
     * Afficher notification de ban (15 min)
     */
    private void showBanNotification(LoginStatus status, String email) {
        // Only update button if typed email matches
        if (!emailField.getText().trim().equals(email)) return;

        loginBtn.setDisable(true);

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    LoginStatus current = attemptService.checkLoginAllowed(email);
                    if (current.allowed) {
                        loginBtn.setDisable(false);
                        loginBtn.setText("Sign In");
                        loginBtn.setStyle("-fx-background-color: #7B5FF5; -fx-text-fill: white;");
                    } else {
                        loginBtn.setText("Banned (" + current.getFormattedTime() + ")");
                    }
                })
        );

        timeline.setCycleCount((int) status.secondsRemaining + 1);
        timeline.play();
    }

    @FXML
    private void goToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserSignUP.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showAlert("Error loading signup page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDashboard(User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
        Parent root = loader.load();

        DashboardController controller = loader.getController();
        controller.setCurrentUser(user);

        Stage stage = (Stage) loginBtn.getScene().getWindow();
        stage.close();

        Stage dashboardStage = new Stage();
        dashboardStage.initStyle(StageStyle.UNDECORATED);
        dashboardStage.setScene(new Scene(root));
        dashboardStage.setMaximized(true);
        dashboardStage.setResizable(false);

        dashboardStage.show();

        System.out.println("✅ Dashboard loaded for: " + user.getName());
    }

    private void loadMainApp(User user) throws Exception {
        showSuccess("Regular user dashboard coming soon!\nWelcome, " + user.getName());
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordVisible.setVisible(true);
            eyeIcon.setText("🙈");
        } else {
            passwordField.setVisible(true);
            passwordVisible.setVisible(false);
            eyeIcon.setText("👁");
        }
    }

    @FXML
    private void handleForgotPassword() {
        System.out.println("🔐 Opening Forgot Password dialog...");
        try {
            Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
            ForgotPasswordDialog dialog = new ForgotPasswordDialog();
            dialog.show(stage);
        } catch (Exception e) {
            showAlert("Error opening forgot password: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    @FXML private void handleMinimize() {
        Stage stage = (Stage) minimizeBtn.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) maximizeBtn.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML private void onCloseHover() {
        closeBtn.setStyle("-fx-background-color: rgba(196, 43, 28, 0.9); -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 0;");
    }

    @FXML private void onCloseExit() {
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 14; -fx-cursor: hand;");
    }

    @FXML private void onMinimizeHover() {
        minimizeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 0;");
    }

    @FXML private void onMinimizeExit() {
        minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 14; -fx-cursor: hand;");
    }

    @FXML private void onMaximizeHover() {
        maximizeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 0;");
    }

    @FXML private void onMaximizeExit() {
        maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 14; -fx-cursor: hand;");
    }

    private void showAlert(String message) {
        showToast(message, "error");
    }

    private void showSuccess(String message) {
        showToast(message, "success");
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showToast(String message, String type) {
        javafx.scene.Parent root = loginBtn.getParent();
        while (root != null && !(root instanceof javafx.scene.layout.AnchorPane)) {
            root = root.getParent();
        }

        if (root == null) return;

        javafx.scene.layout.AnchorPane formContainer = (javafx.scene.layout.AnchorPane) root;
        javafx.scene.layout.VBox toast = new javafx.scene.layout.VBox();
        toast.setAlignment(javafx.geometry.Pos.CENTER);
        toast.setPadding(new javafx.geometry.Insets(16, 24, 16, 24));
        toast.setSpacing(8);
        toast.setMaxWidth(380);

        if (type.equals("error")) {
            toast.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 12; -fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");
        } else {
            toast.setStyle("-fx-background-color: #d1fae5; -fx-background-radius: 12; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");
        }

        javafx.scene.layout.HBox content = new javafx.scene.layout.HBox();
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setSpacing(12);

        Label icon = new Label(type.equals("error") ? "⚠️" : "✅");
        icon.setStyle("-fx-font-size: 20;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + (type.equals("error") ? "#dc2626" : "#047857") + "; -fx-font-weight: 600;");

        content.getChildren().addAll(icon, messageLabel);
        toast.getChildren().add(content);

        javafx.scene.layout.AnchorPane.setTopAnchor(toast, 20.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(toast, 60.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(toast, 60.0);

        formContainer.getChildren().add(toast);

        toast.setOpacity(0);
        toast.setTranslateY(-20);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        javafx.animation.TranslateTransition slideDown = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), toast);
        slideDown.setFromY(-20);
        slideDown.setToY(0);

        fadeIn.play();
        slideDown.play();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));
        pause.setOnFinished(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            javafx.animation.TranslateTransition slideUp = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), toast);
            slideUp.setFromY(0);
            slideUp.setToY(-20);

            fadeOut.play();
            slideUp.play();

            fadeOut.setOnFinished(ev -> formContainer.getChildren().remove(toast));
        });
        pause.play();
    }

    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        passwordVisible.clear();
    }
    @FXML
    private Button faceIDLoginBtn;

    @FXML
    private void handleFaceIDLogin() {
        System.out.println("🔐 Opening Face ID ...");

        try {
            Stage stage = (Stage) faceIDLoginBtn.getScene().getWindow();

            FaceIDDialogController.show(stage);

            // Wait a little to check session after dialog closes
            javafx.animation.PauseTransition delay =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));

            delay.setOnFinished(event -> {
                User user = Session.getInstance().getCurrentUser();

                if (user != null) {
                    try {
                        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            loadDashboard(user);
                        } else {
                            loadMainApp(user);
                        }
                    } catch (Exception e) {
                        showAlert("Navigation error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            delay.play();

        } catch (Exception e) {
            showAlert("Face ID error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}