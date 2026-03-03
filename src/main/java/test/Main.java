package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import model.User;
import service.UserService;

import java.sql.SQLException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/User.fxml"));
        Scene scene = new Scene(loader.load());

        // Remove default OS window borders (since you use custom buttons)
        stage.initStyle(StageStyle.UNDECORATED);

        stage.setTitle("LAMMA Adventure");

        stage.setScene(scene);

        // ✅ OPEN FULL SCREEN
        stage.setFullScreen(true);

        // ✅ Remove ESC message
        stage.setFullScreenExitHint("");

        stage.show();
    }


    public static void main(String[] args) {

        // Launch JavaFX
        launch(args);


        // ------------------ OPTIONAL DATABASE TEST ------------------

        UserService us = new UserService();

        try {
            User u1 = new User(
                    "Saif",
                    "saif@gmail.com",
                    "123456",
                    "USER",
                    "12345678",
                    "NO",
                    null
            );
            us.ajouter(u1);

            System.out.println(us.recuperer());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

}