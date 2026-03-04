package controllers;

import Services.UtilisateurService;
import entities.Utilisateur;
import utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UtilisateurService utilisateurService;

    @FXML
    public void initialize() {
        utilisateurService = new UtilisateurService();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        Utilisateur u = utilisateurService.login(email.trim(), password.trim());
        
        if (u != null) {
            Session.getInstance().setLoggedUser(u);
            errorLabel.setVisible(false);
            openMainView();
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("LAMMA Voyage - Dashboard");
            stage.setScene(new Scene(root, 1200, 750));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'application.");
        }
    }
}
