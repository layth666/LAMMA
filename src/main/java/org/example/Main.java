package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Test de la connexion à la base de données avant de démarrer l'UI
            System.out.println("=== DÉMARRAGE DE L'APPLICATION ===");
            System.out.println("Test de connexion à la base de données...");

            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("✓ Connexion à la base de données établie avec succès !");
                System.out.println("  Base de données: " + conn.getCatalog());
            } catch (SQLException e) {
                System.err.println("✗ Échec de la connexion à la base de données !");
                System.err.println("  Vérifiez que MySQL est démarré et que les identifiants sont corrects.");
                System.err.println("  URL: jdbc:mysql://localhost:3306/camping_sponsors_db");
                System.err.println("  User: root");
                System.err.println("  Password: [vide]");
                e.printStackTrace();

                // Afficher une alerte mais continuer le démarrage
                System.err.println("⚠ L'application va démarrer mais les fonctionnalités seront limitées.");
            }

            // Chargement du FXML
            System.out.println("Chargement de l'interface utilisateur...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("✗ Erreur: Fichier FXML non trouvé !");
                System.err.println("  Chemin recherché: /view/SponsorView.fxml");
                System.err.println("  Vérifiez que le fichier est dans src/main/resources/view/");
                return;
            }

            Parent root = loader.load();
            System.out.println("✓ Interface chargée avec succès");

            // Configuration de la scène
            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setTitle("Gestion des Sponsors & Partenariats");
            primaryStage.setScene(scene);
            primaryStage.show();

            System.out.println("✓ Application démarrée avec succès !");
            System.out.println("================================");

        } catch (Exception e) {
            System.err.println("✗ Erreur fatale au démarrage !");
            System.err.println("  Message: " + e.getMessage());
            System.err.println("  Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "inconnue"));
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("=== ARRÊT DE L'APPLICATION ===");
        System.out.println("✓ Application arrêtée proprement");
        super.stop();
    }

    public static void main(String[] args) {
        System.out.println("=== INITIALISATION ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Démarrage de l'application...");

        launch(args);
    }
}