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

public class VerifyCodeDialog {

    private Stage dialogStage;
    private User user;
    private String correctCode;
    private String userEmail;

    public VerifyCodeDialog(User user, String correctCode, String userEmail) {
        this.user = user;
        this.correctCode = correctCode;
        this.userEmail = userEmail;
    }

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
                "-fx-background-color: linear-gradient(to right,#10b981,#059669);" +
                        "-fx-background-radius:16 16 0 0;"
        );

        Label title = new Label("📬 Verify Code");
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
        content.setAlignment(Pos.CENTER);

        Label instruction = new Label("Enter the 6-digit code sent to " + maskEmail(userEmail));
        instruction.setStyle("-fx-text-fill:#bfc7d5; -fx-font-weight:bold;");

        HBox codeBox = new HBox(12);
        codeBox.setAlignment(Pos.CENTER);

        TextField[] fields = new TextField[6];
        for (int i = 0; i < 6; i++) {
            TextField f = new TextField();
            f.setPrefSize(60, 60);
            f.setStyle(
                    "-fx-background-color:#2b2b3c;" +
                            "-fx-text-fill:white;" +
                            "-fx-font-size:24;" +
                            "-fx-alignment:center;" +
                            "-fx-background-radius:10;" +
                            "-fx-border-radius:10;"
            );

            int idx = i;
            f.textProperty().addListener((obs, oldV, newV) -> {
                if (newV.length() > 1) f.setText(newV.substring(0,1));
                if (!newV.matches("[0-9]*")) f.setText(oldV);
                if (newV.length()==1 && idx<5) fields[idx+1].requestFocus();
            });

            fields[i] = f;
            codeBox.getChildren().add(f);
        }

        Button verifyBtn = new Button("Verify Code");
        verifyBtn.setPrefWidth(440);
        verifyBtn.setPrefHeight(48);
        verifyBtn.setStyle(
                "-fx-background-color:#10b981;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-cursor:hand;"
        );

        verifyBtn.setOnAction(e -> handleVerify(fields, content));

        content.getChildren().addAll(instruction, codeBox, verifyBtn);
        return content;
    }

    private void handleVerify(TextField[] fields, VBox content) {
        StringBuilder code = new StringBuilder();
        for (TextField f : fields) code.append(f.getText());

        if (code.length() != 6) {
            showMessage(content,"Enter all 6 digits","error");
            return;
        }

        if (code.toString().equals(correctCode)) {
            showMessage(content,"Code verified!","success");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                dialogStage.close();
                ResetPasswordDialog resetDialog = new ResetPasswordDialog(user);
                resetDialog.show((Stage) dialogStage.getOwner());
            });
            pause.play();
        } else {
            showMessage(content,"Invalid code. Try again","error");
            for (TextField f : fields) f.clear();
            fields[0].requestFocus();
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String u = parts[0], d = parts[1];
        return u.length() <=2 ? u+"@"+d : u.substring(0,2)+"***@"+d;
    }

    private void showMessage(VBox container, String message, String type) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color:"+ (type.equals("error")?"#3a1f1f":"#1f3a2a")+"; -fx-background-radius:10;");
        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill:"+ (type.equals("error")?"#ff6b6b":"#00ffae")+"; -fx-font-weight:bold;");
        box.getChildren().add(lbl);

        if(container.getChildren().size() > 3) container.getChildren().remove(3);
        container.getChildren().add(box);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), box);
        fade.setFromValue(0); fade.setToValue(1);
        fade.play();
    }
}