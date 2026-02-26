package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import controllers.EquipementListeController;
import controllers.EquipementStoreController;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private Button btnEquipements;
    @FXML private Button btnBoutique;
    @FXML private Button btnMessagerie;
    @FXML private Button btnMinimize;
    @FXML private Button btnMaximize;
    @FXML private Button btnQuitter;
    @FXML private ImageView logoView;

    // ✅ Utiliser Region au lieu de Node
    private Region equipementView;
    private Region boutiqueView;
    private Region chatView;

    // Références des contrôleurs pour synchroniser les vues
    private EquipementListeController equipementListeController;
    private EquipementStoreController equipementStoreController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Charger le logo
        try {
            URL logoUrl = getClass().getResource("/images/lamma-logo.png");
            if (logoUrl != null) {
                Image logo = new Image(logoUrl.toExternalForm());
                logoView.setImage(logo);
                logoView.setVisible(true);
            } else {
                logoView.setVisible(false);
            }
        } catch (Exception e) {
            logoView.setVisible(false);
        }

        // Charger les vues
        loadViews();

        // Afficher équipements par défaut
        showEquipements();
    }

    private void loadViews() {
        try {
            // Charger EquipementView (Dashboard admin)
            FXMLLoader equipementLoader =
                    new FXMLLoader(getClass().getResource("/views/EquipementListeView.fxml"));
            equipementView = equipementLoader.load();
            equipementView.setMaxWidth(Double.MAX_VALUE);
            equipementView.setMaxHeight(Double.MAX_VALUE);
            equipementListeController = equipementLoader.getController();

            // Charger EquipementStoreView (Boutique)
            FXMLLoader boutiqueLoader = new FXMLLoader(getClass().getResource("/views/EquipementStoreView.fxml"));
            boutiqueView = boutiqueLoader.load();
            boutiqueView.setMaxWidth(Double.MAX_VALUE);
            boutiqueView.setMaxHeight(Double.MAX_VALUE);
            equipementStoreController = boutiqueLoader.getController();

            // Charger ChatView
            FXMLLoader chatLoader =
                    new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            chatView = chatLoader.load();
            chatView.setMaxWidth(Double.MAX_VALUE);
            chatView.setMaxHeight(Double.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des vues:");
            e.printStackTrace();
        }
    }

    @FXML
    private void showEquipements() {
        if (equipementView != null) {
            // Rafraîchir les données à chaque affichage pour rester synchronisé avec la boutique
            if (equipementListeController != null) {
                equipementListeController.rafraichirDepuisMain();
            }
            contentPane.getChildren().clear();
            contentPane.getChildren().add(equipementView);
            updateNavButtons(btnEquipements);
        }
    }

    @FXML
    private void showBoutique() {
        if (boutiqueView != null) {
            // Rafraîchir les données à chaque affichage pour rester synchronisé avec le dashboard
            if (equipementStoreController != null) {
                equipementStoreController.rafraichirDepuisMain();
            }
            contentPane.getChildren().clear();
            contentPane.getChildren().add(boutiqueView);
            updateNavButtons(btnBoutique);
        }
    }

    @FXML
    private void showMessagerie() {
        if (chatView != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(chatView);
            updateNavButtons(btnMessagerie);
        }
    }

    private void updateNavButtons(Button activeButton) {
        btnEquipements.getStyleClass().remove("active");
        btnBoutique.getStyleClass().remove("active");
        btnMessagerie.getStyleClass().remove("active");

        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private Stage getStage() {
        return (Stage) contentPane.getScene().getWindow();
    }

    @FXML
    private void minimizeWindow() {
        getStage().setIconified(true);
    }

    @FXML
    private void toggleMaximize() {
        Stage stage = getStage();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void quitter() {
        getStage().close();
    }
}
