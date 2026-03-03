package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.User;
import service.CloudinaryService;
import service.UserService;
import utils.PasswordHasher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Random;
import java.util.regex.Pattern;

public class SignupController {

    @FXML private TextField UserName;
    @FXML private TextField email;
    @FXML private TextField phone;
    @FXML private PasswordField password;
    @FXML private TextField passwordVisible;
    @FXML private ComboBox<String> motorizedCombo;
    @FXML private ImageView profileImageView;
    @FXML private Button uploadPhotoBtn;
    @FXML private Button Signup;
    @FXML private Button togglePasswordBtn;
    @FXML private Label eyeIcon;
    @FXML private Hyperlink loginLink;

    // Error labels
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label passwordStrength;

    // Window controls
    @FXML private Button closeBtn;
    @FXML private Button minimizeBtn;
    @FXML private Button maximizeBtn;

    // ✅ SIMPLE CAPTCHA (No WebView needed!)
    @FXML private TextField captchaInput;
    @FXML private Label captchaQuestion;
    @FXML private HBox captchaContainer;

    private double xOffset = 0;
    private double yOffset = 0;

    private UserService userService = new UserService();
    private File selectedImageFile = null;

    // Simple Math CAPTCHA
    private int captchaAnswer = 0;

    // REGEX
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");
    private final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{8,15}$");

    @FXML
    public void initialize() {
        setupWindowDragging();
        setupRealtimeValidation();
        setupMotorizedCombo();
        setupProfileImageUpload();
        generateCaptcha();
    }

    private void setupWindowDragging() {
        javafx.scene.layout.HBox topBar = (javafx.scene.layout.HBox) closeBtn.getParent();

        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        topBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) topBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private void setupMotorizedCombo() {
        if (motorizedCombo != null) {
            motorizedCombo.getItems().addAll("NO", "YES");
            motorizedCombo.setValue("NO");
        }
    }

    private void setupProfileImageUpload() {
        if (uploadPhotoBtn != null) {
            uploadPhotoBtn.setOnAction(e -> handleImageUpload());
        }
    }

    /**
     * ✅ GENERATE SIMPLE MATH CAPTCHA
     * No WebView, no JavaScript, 100% reliable!
     */
    private void generateCaptcha() {
        Random random = new Random();
        int num1 = random.nextInt(10) + 1;
        int num2 = random.nextInt(10) + 1;

        String[] operations = {"+", "-", "×"};
        String operation = operations[random.nextInt(operations.length)];

        switch (operation) {
            case "+":
                captchaAnswer = num1 + num2;
                break;
            case "-":
                // Make sure result is positive
                if (num1 < num2) {
                    int temp = num1;
                    num1 = num2;
                    num2 = temp;
                }
                captchaAnswer = num1 - num2;
                break;
            case "×":
                captchaAnswer = num1 * num2;
                break;
        }

        captchaQuestion.setText(String.format("What is %d %s %d ?", num1, operation, num2));
        captchaInput.clear();

        System.out.println("✅ CAPTCHA generated: " + num1 + " " + operation + " " + num2 + " = " + captchaAnswer);
    }

    /**
     * Verify CAPTCHA answer
     */
    private boolean verifyCaptcha() {
        try {
            String userAnswer = captchaInput.getText().trim();
            if (userAnswer.isEmpty()) {
                return false;
            }

            int answer = Integer.parseInt(userAnswer);
            return answer == captchaAnswer;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadPhotoBtn.getScene().getWindow());

        if (selectedFile != null) {
            try {
                selectedImageFile = selectedFile;
                Image image = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(image);
                System.out.println("✅ Image selected: " + selectedFile.getName());
                showSuccess("Image selected!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error loading image: " + ex.getMessage());
            }
        }
    }

    private void setupRealtimeValidation() {
        password.textProperty().bindBidirectional(passwordVisible.textProperty());

        UserName.textProperty().addListener((obs, old, newVal) -> validateUsername(newVal));
        email.textProperty().addListener((obs, old, newVal) -> validateEmail(newVal));
        phone.textProperty().addListener((obs, old, newVal) -> validatePhone(newVal));
        password.textProperty().addListener((obs, old, newVal) -> {
            validatePassword(newVal);
            updatePasswordStrength(newVal);
        });

        Signup.setOnMouseEntered(e -> {
            if (!Signup.isDisabled()) {
                Signup.setStyle("-fx-background-color: #6B4FE5; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        });

        Signup.setOnMouseExited(e -> {
            if (!Signup.isDisabled()) {
                Signup.setStyle("-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        });
    }

    private void validatePhone(String phoneText) {
        if (phoneText.isEmpty()) {
            phoneError.setVisible(false);
            phone.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else if (!PHONE_PATTERN.matcher(phoneText).matches()) {
            phoneError.setText("Phone must be 8-15 digits");
            phoneError.setVisible(true);
            phone.setStyle("-fx-background-color: white; -fx-border-color: #fc8181; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else {
            phoneError.setVisible(false);
            phone.setStyle("-fx-background-color: white; -fx-border-color: #48bb78; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        }
    }

    private void validateUsername(String username) {
        if (username.isEmpty()) {
            usernameError.setVisible(false);
            UserName.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else if (username.length() < 5) {
            usernameError.setText("Username must be at least 5 characters");
            usernameError.setVisible(true);
            UserName.setStyle("-fx-background-color: white; -fx-border-color: #fc8181; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else {
            usernameError.setVisible(false);
            UserName.setStyle("-fx-background-color: white; -fx-border-color: #48bb78; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        }
    }

    private void validateEmail(String emailText) {
        if (emailText.isEmpty()) {
            emailError.setVisible(false);
            email.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else if (!EMAIL_PATTERN.matcher(emailText).matches()) {
            emailError.setText("Invalid email format");
            emailError.setVisible(true);
            email.setStyle("-fx-background-color: white; -fx-border-color: #fc8181; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        } else {
            emailError.setVisible(false);
            email.setStyle("-fx-background-color: white; -fx-border-color: #48bb78; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        }
    }

    private void validatePassword(String passwordText) {
        String username = UserName.getText();
        String borderStyle;

        if (passwordText.isEmpty()) {
            passwordError.setVisible(false);
            borderStyle = "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;";
        } else if (!PASSWORD_PATTERN.matcher(passwordText).matches()) {
            passwordError.setText("Must have uppercase, lowercase, special char, min 6 chars");
            passwordError.setVisible(true);
            borderStyle = "-fx-background-color: white; -fx-border-color: #fc8181; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;";
        } else if (!username.isEmpty() && passwordText.toLowerCase().contains(username.toLowerCase())) {
            passwordError.setText("Password must not contain username");
            passwordError.setVisible(true);
            borderStyle = "-fx-background-color: white; -fx-border-color: #fc8181; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;";
        } else {
            passwordError.setVisible(false);
            borderStyle = "-fx-background-color: white; -fx-border-color: #48bb78; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;";
        }

        password.setStyle(borderStyle);
        passwordVisible.setStyle(borderStyle);
    }

    private void updatePasswordStrength(String passwordText) {
        if (passwordText.isEmpty()) {
            passwordStrength.setText("");
            return;
        }

        int strength = 0;
        if (passwordText.length() >= 8) strength++;
        if (passwordText.matches(".*[a-z].*")) strength++;
        if (passwordText.matches(".*[A-Z].*")) strength++;
        if (passwordText.matches(".*[0-9].*")) strength++;
        if (passwordText.matches(".*[@#$%^&+=!].*")) strength++;

        if (strength <= 2) {
            passwordStrength.setText("Weak");
            passwordStrength.setStyle("-fx-text-fill: #fc8181; -fx-font-size: 10; -fx-font-weight: 600;");
        } else if (strength == 3 || strength == 4) {
            passwordStrength.setText("Medium");
            passwordStrength.setStyle("-fx-text-fill: #f6ad55; -fx-font-size: 10; -fx-font-weight: 600;");
        } else {
            passwordStrength.setText("Strong ✓");
            passwordStrength.setStyle("-fx-text-fill: #48bb78; -fx-font-size: 10; -fx-font-weight: 600;");
        }
    }

    /**
     * ✅ SIGNUP WITH SIMPLE CAPTCHA
     */
    @FXML
    public void handleSignup() {
        String username = UserName.getText();
        String userEmail = email.getText();
        String userPhone = phone.getText();
        String userPassword = password.getText();
        String motorized = motorizedCombo.getValue();

        // Validation
        if (username.length() < 5) {
            showAlert("Username must contain at least 5 characters");
            return;
        }

        if (!EMAIL_PATTERN.matcher(userEmail).matches()) {
            showAlert("Invalid email format");
            return;
        }

        if (!PHONE_PATTERN.matcher(userPhone).matches()) {
            showAlert("Phone number must be 8-15 digits");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(userPassword).matches()) {
            showAlert("Password must contain:\n- Uppercase letter\n- Lowercase letter\n- Special character\n- Minimum 6 characters");
            return;
        }

        if (userPassword.toLowerCase().contains(username.toLowerCase())) {
            showAlert("Password must not contain username");
            return;
        }

        // ✅ VERIFY CAPTCHA
        if (!verifyCaptcha()) {
            showAlert("Incorrect CAPTCHA answer. Please try again.");
            captchaInput.setStyle("-fx-background-color: white; -fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
            generateCaptcha(); // Generate new one
            return;
        }

        // Reset CAPTCHA field style
        captchaInput.setStyle("-fx-background-color: white; -fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");

        // Disable button
        Signup.setDisable(true);
        Signup.setText("Creating account...");

        // Upload and create account
        new Thread(() -> {
            try {
                String imageUrl = null;

                if (selectedImageFile != null) {
                    System.out.println("📤 Uploading image to Cloudinary...");
                    Platform.runLater(() -> showSuccess("Uploading image..."));
                    imageUrl = CloudinaryService.uploadImage(selectedImageFile);
                    System.out.println("✅ Image uploaded: " + imageUrl);
                }

                String hashedPassword = PasswordHasher.hashPassword(userPassword);

                User user = new User(
                        username,
                        userEmail,
                        hashedPassword,
                        "USER",
                        userPhone,
                        motorized,
                        imageUrl
                );

                userService.ajouter(user);

                Platform.runLater(() -> {
                    Signup.setDisable(false);
                    Signup.setText("Sign Up");

                    showSuccess("Account created successfully! 🎉");
                    clearFields();

                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                    delay.setOnFinished(event -> goToLogin());
                    delay.play();
                });

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    Signup.setDisable(false);
                    Signup.setText("Sign Up");
                    showAlert("Database error: " + e.getMessage());
                    generateCaptcha(); // Generate new CAPTCHA
                });
                e.printStackTrace();

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Signup.setDisable(false);
                    Signup.setText("Sign Up");
                    showAlert("Error: " + e.getMessage());
                    generateCaptcha(); // Generate new CAPTCHA
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    @FXML private void handleMinimize() {
        Stage stage = (Stage) minimizeBtn.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML private void handleMaximize() {
        Stage stage = (Stage) maximizeBtn.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) Signup.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showAlert("Error loading login page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        if (password.isVisible()) {
            password.setVisible(false);
            passwordVisible.setVisible(true);
            eyeIcon.setText("🙈");
        } else {
            password.setVisible(true);
            passwordVisible.setVisible(false);
            eyeIcon.setText("👁");
        }
    }

    @FXML private void onCloseHover() {
        closeBtn.setStyle("-fx-background-color: #c42b1c; -fx-text-fill: white; -fx-font-size: 12; -fx-cursor: hand;");
    }

    @FXML private void onCloseExit() {
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 12; -fx-cursor: hand;");
    }

    @FXML private void onMinimizeHover() {
        minimizeBtn.setStyle("-fx-background-color: #e5e5e5; -fx-text-fill: #000000; -fx-font-size: 12; -fx-cursor: hand;");
    }

    @FXML private void onMinimizeExit() {
        minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 12; -fx-cursor: hand;");
    }

    @FXML private void onMaximizeHover() {
        maximizeBtn.setStyle("-fx-background-color: #e5e5e5; -fx-text-fill: #000000; -fx-font-size: 12; -fx-cursor: hand;");
    }

    @FXML private void onMaximizeExit() {
        maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 12; -fx-cursor: hand;");
    }

    private void showAlert(String message) {
        showToast(message, "error");
    }

    private void showSuccess(String message) {
        showToast(message, "success");
    }

    private void showToast(String message, String type) {
        javafx.scene.Parent root = Signup.getParent();
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

        if (type.equals("error")) {
            javafx.animation.Timeline shake = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(0), new javafx.animation.KeyValue(toast.translateXProperty(), 0)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(100), new javafx.animation.KeyValue(toast.translateXProperty(), 10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150), new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), new javafx.animation.KeyValue(toast.translateXProperty(), 10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), new javafx.animation.KeyValue(toast.translateXProperty(), 0))
            );
            shake.play();
        }

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
        UserName.clear();
        email.clear();
        phone.clear();
        password.clear();
        motorizedCombo.setValue("NO");
        selectedImageFile = null;
        if (profileImageView != null) {
            profileImageView.setImage(null);
        }
        captchaInput.clear();
        captchaInput.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13;");
        generateCaptcha();
    }
}