package controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.User;
import service.EmailService;
import service.UserService;

import java.sql.SQLException;

public class ForgotPasswordDialog {

    private Stage dialogStage;
    private UserService userService = new UserService();
    private String verificationCode;
    private String userEmail;

    public void show(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox mainContainer = new VBox();
        mainContainer.setStyle(
                "-fx-background-color: #1e1e2f;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-color:#2b2b3c;" +
                        "-fx-border-width:1;"
        );
        mainContainer.setPrefWidth(500);
        mainContainer.setMinHeight(360);

        HBox header = createHeader();
        VBox content = createContent();

        mainContainer.getChildren().addAll(header, content);

        Scene scene = new Scene(new StackPane(mainContainer));
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialogStage.setScene(scene);
        javafx.application.Platform.runLater(() -> {
            dialogStage.show();
            dialogStage.centerOnScreen();
            dialogStage.toFront();
        });
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right,#7B5FF5,#6B4FE5);" +
                        "-fx-background-radius:16 16 0 0;"
        );

        Label title = new Label("🔐 Forgot Password?");
        title.setStyle(
                "-fx-font-size:22;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:white;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("✕");
        close.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:18;" +
                        "-fx-cursor:hand;"
        );
        close.setOnAction(e -> dialogStage.close());

        header.getChildren().addAll(title, spacer, close);
        return header;
    }

    private VBox createContent() {
        VBox content = new VBox(18);
        content.setPadding(new Insets(30));

        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-text-fill:#bfc7d5;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        styleField(emailField);

        Button sendBtn = new Button("Send Verification Code");
        sendBtn.setPrefWidth(440);
        sendBtn.setPrefHeight(48);
        sendBtn.setStyle(
                "-fx-background-color:#7B5FF5;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-cursor:hand;"
        );

        sendBtn.setOnAction(e -> handleSend(emailField.getText().trim(), content));

        content.getChildren().addAll(emailLabel, emailField, sendBtn);
        return content;
    }

    private void styleField(TextField field) {
        field.setPrefHeight(48);
        field.setStyle(
                "-fx-background-color:#2b2b3c;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-prompt-text-fill:#888;" +
                        "-fx-padding:12;"
        );
    }

    private void handleSend(String email, VBox content) {
        if (email.isEmpty()) {
            showMessage(content, "Please enter your email", "error");
            return;
        }

        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                showMessage(content, "No account found with this email", "error");
                return;
            }

            verificationCode = EmailService.generateVerificationCode();
            userEmail = email;

            boolean sent = EmailService.sendPasswordResetEmail(email, user.getName(), verificationCode);

            if (sent) {
                showMessage(content, "Verification code sent! Check your email 📧", "success");
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(ev -> {
                    dialogStage.close();
                    VerifyCodeDialog verifyDialog = new VerifyCodeDialog(user, verificationCode, userEmail);
                    verifyDialog.show((Stage) dialogStage.getOwner());
                });
                pause.play();
            } else {
                showMessage(content, "Failed to send email", "error");
            }
        } catch (SQLException e) {
            showMessage(content, e.getMessage(), "error");
        }
    }

    private void showMessage(VBox container, String message, String type) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setStyle(
                "-fx-background-color:" + (type.equals("error") ? "#3a1f1f" : "#1f3a2a") + ";" +
                        "-fx-background-radius:10;"
        );

        Label msg = new Label(message);
        msg.setStyle(
                "-fx-text-fill:" + (type.equals("error") ? "#ff6b6b" : "#00ffae") + ";" +
                        "-fx-font-weight:bold;"
        );

        box.getChildren().add(msg);

        if (container.getChildren().size() > 3)
            container.getChildren().remove(3);
        container.getChildren().add(box);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), box);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}