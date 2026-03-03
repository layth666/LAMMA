package controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import model.User;
import service.UserService;
import utils.PasswordHasher;

import java.sql.SQLException;

public class ResetPasswordDialog {

    private Stage dialogStage;
    private User user;
    private UserService userService = new UserService();

    public ResetPasswordDialog(User user) {
        this.user = user;
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
        mainContainer.setMinHeight(420);
        mainContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mainContainer.setMaxHeight(Region.USE_PREF_SIZE);

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

        Label title = new Label("🔒 Reset Password");

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


        Label passwordLabel = new Label("New Password");

        passwordLabel.setStyle("-fx-text-fill:#bfc7d5;");


        PasswordField passwordField = new PasswordField();

        passwordField.setPromptText("Enter new password");

        styleField(passwordField);



        Label confirmLabel = new Label("Confirm Password");

        confirmLabel.setStyle("-fx-text-fill:#bfc7d5;");



        PasswordField confirmField = new PasswordField();

        confirmField.setPromptText("Confirm password");

        styleField(confirmField);



        Label strengthLabel = new Label();

        strengthLabel.setStyle("-fx-font-weight:bold;");



        passwordField.textProperty().addListener((obs,o,n)->{

            updatePasswordStrength(n,strengthLabel);

        });



        Button resetBtn = new Button("Reset Password");

        resetBtn.setPrefHeight(48);

        resetBtn.setPrefWidth(440);

        resetBtn.setStyle(
                "-fx-background-color:#7B5FF5;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-cursor:hand;"
        );


        resetBtn.setOnAction(e ->

                handleReset(

                        passwordField.getText(),

                        confirmField.getText(),

                        content

                )

        );


        content.getChildren().addAll(

                passwordLabel,

                passwordField,

                strengthLabel,

                confirmLabel,

                confirmField,

                resetBtn

        );


        return content;

    }



    private void styleField(TextField field){

        field.setPrefHeight(48);

        field.setStyle(

                "-fx-background-color:#2b2b3c;" +

                        "-fx-text-fill:white;" +

                        "-fx-background-radius:10;" +

                        "-fx-prompt-text-fill:#888;" +

                        "-fx-padding:12;"

        );

    }



    private void updatePasswordStrength(String password, Label label){

        if(password.length()<6){

            label.setText("Weak");

            label.setStyle("-fx-text-fill:red;");

        }

        else if(password.length()<10){

            label.setText("Medium");

            label.setStyle("-fx-text-fill:orange;");

        }

        else{

            label.setText("Strong");

            label.setStyle("-fx-text-fill:#00ffae;");

        }

    }



    private void handleReset(String password,String confirm,VBox content){

        if(password.isEmpty()||confirm.isEmpty()){

            showMessage(content,"Fill all fields","error");

            return;

        }


        if(!password.equals(confirm)){

            showMessage(content,"Passwords do not match","error");

            return;

        }


        try{

            String hashed=PasswordHasher.hashPassword(password);

            user.setPassword(hashed);

            userService.modifier(user);

            showMessage(content,"Password reset successful","success");


            PauseTransition pause=new PauseTransition(Duration.seconds(2));

            pause.setOnFinished(e-> dialogStage.close());

            pause.play();

        }

        catch(SQLException e){

            showMessage(content,e.getMessage(),"error");

        }

    }




    private void showMessage(VBox container,String message,String type){

        HBox box=new HBox(10);

        box.setAlignment(Pos.CENTER_LEFT);

        box.setPadding(new Insets(12));

        box.setStyle(

                "-fx-background-color:"+

                        (type.equals("error")?"#3a1f1f":"#1f3a2a")+";"+

                        "-fx-background-radius:10;"

        );


        Label msg=new Label(message);

        msg.setStyle(

                "-fx-text-fill:"+

                        (type.equals("error")?"#ff6b6b":"#00ffae")+";"+

                        "-fx-font-weight:bold;"

        );


        box.getChildren().add(msg);


        if(container.getChildren().size()>6)

            container.getChildren().remove(6);


        container.getChildren().add(box);



        FadeTransition fade=new FadeTransition(Duration.seconds(0.5),box);

        fade.setFromValue(0);

        fade.setToValue(1);

        fade.play();

    }

}