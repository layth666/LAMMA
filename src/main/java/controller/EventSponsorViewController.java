package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import model.EventSponsor;
import model.Evenement;
import model.Sponsor;
import utils.DatabaseConnection;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class EventSponsorViewController implements Initializable {

    @FXML
    private ComboBox<Evenement> cboEvenement;
    @FXML
    private ComboBox<Sponsor> cboSponsor;
    @FXML
    private ComboBox<String> cboNiveau;
    @FXML
    private TextField txtMontant;
    @FXML
    private ListView<EventSponsor> listAssociations;

    private ObservableList<Evenement> evenementList = FXCollections.observableArrayList();
    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private ObservableList<EventSponsor> associationList = FXCollections.observableArrayList();
    private EventSponsor selectedAssociation = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listAssociations.setCellFactory(lv -> new EventSponsorListCell(this::onModifierClick, this::onSupprimerClick));

        cboNiveau.setItems(FXCollections.observableArrayList("GOLD", "SILVER", "BRONZE", "PARTENAIRE"));

        loadEvenements();
        loadSponsors();
        loadAssociations();
    }

    private void onSupprimerClick(EventSponsor es) {
        supprimerAssociation(es);
    }

    @FXML
    private void supprimerAssociation() {
        supprimerAssociation(        listAssociations.getSelectionModel().getSelectedItem());
    }

    private void onModifierClick(EventSponsor es) {
        selectedAssociation = es;
        listAssociations.getSelectionModel().select(es);
        // Remplir le formulaire
        evenementList.stream().filter(e -> e.getId() == es.getEventId()).findFirst().ifPresent(cboEvenement::setValue);
        sponsorList.stream().filter(s -> s.getId() == es.getSponsorId()).findFirst().ifPresent(cboSponsor::setValue);
        cboNiveau.setValue(es.getNiveau());
        txtMontant.setText(String.format("%.2f", es.getMontant()));
    }

    @FXML
    private void associerSponsor() {
        if (selectedAssociation != null) {
            showAlert("Attention", "Vous êtes en mode modification. Annulez d'abord ou enregistrez.");
            return;
        }
        Evenement evenement = cboEvenement.getValue();
        Sponsor sponsor = cboSponsor.getValue();
        String niveau = cboNiveau.getValue();
        String montantStr = txtMontant.getText().trim();

        if (evenement == null || sponsor == null || niveau == null || montantStr.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        try {
            double montant = Double.parseDouble(montantStr);

            String query = "INSERT INTO EventSponsor (event_id, sponsor_id, niveau, montant) VALUES (?, ?, ?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, evenement.getId());
                pstmt.setInt(2, sponsor.getId());
                pstmt.setString(3, niveau);
                pstmt.setDouble(4, montant);
                pstmt.executeUpdate();

                loadAssociations();
                clearFields();
                selectedAssociation = null;
                showAlert("Succès", "Association créée avec succès");
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le montant doit être un nombre valide");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                showAlert("Erreur", "Ce sponsor est déjà associé à cet événement");
            } else {
                showAlert("Erreur", "Erreur lors de l'association: " + e.getMessage());
            }
        }
    }

    @FXML
    private void actualiser() {
        loadEvenements();
        loadSponsors();
        loadAssociations();
        selectedAssociation = null;
        clearFields();
    }

    @FXML
    private void modifierAssociation() {
        if (selectedAssociation == null) {
            showAlert("Erreur", "Veuillez sélectionner une association à modifier (cliquez sur Modifier dans la carte)");
            return;
        }
        Evenement evenement = cboEvenement.getValue();
        Sponsor sponsor = cboSponsor.getValue();
        String niveau = cboNiveau.getValue();
        String montantStr = txtMontant.getText().trim();

        if (evenement == null || sponsor == null || niveau == null || montantStr.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        try {
            double montant = Double.parseDouble(montantStr);

            String query = "UPDATE EventSponsor SET event_id = ?, sponsor_id = ?, niveau = ?, montant = ? WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, evenement.getId());
                pstmt.setInt(2, sponsor.getId());
                pstmt.setString(3, niveau);
                pstmt.setDouble(4, montant);
                pstmt.setInt(5, selectedAssociation.getId());
                pstmt.executeUpdate();

                loadAssociations();
                clearFields();
                selectedAssociation = null;
                showAlert("Succès", "Association modifiée avec succès");
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le montant doit être un nombre valide");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                showAlert("Erreur", "Ce sponsor est déjà associé à cet événement");
            } else {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
            }
        }
    }

    private void supprimerAssociation(EventSponsor selected) {
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner une association à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'association");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette association ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM EventSponsor WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();

                loadAssociations();
                showAlert("Succès", "Association supprimée avec succès");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private void loadEvenements() {
        evenementList.clear();
        String query = "SELECT * FROM Evenement ORDER BY date_debut DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                evenementList.add(new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("description"),
                        rs.getString("statut")
                ));
            }
            cboEvenement.setItems(evenementList);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des événements: " + e.getMessage());
        }
    }

    private void loadSponsors() {
        sponsorList.clear();
        String query = "SELECT * FROM Sponsor WHERE statut = true ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sponsorList.add(new Sponsor(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("logo"),
                        rs.getBoolean("statut")
                ));
            }
            cboSponsor.setItems(sponsorList);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des sponsors: " + e.getMessage());
        }
    }

    private void loadAssociations() {
        associationList.clear();
        String query = "SELECT es.*, e.nom as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "ORDER BY e.date_debut DESC, es.niveau";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                EventSponsor es = new EventSponsor(
                        rs.getInt("id"),
                        rs.getString("event_nom"),
                        rs.getString("sponsor_nom"),
                        rs.getString("niveau"),
                        rs.getDouble("montant")
                );
                es.setEventId(rs.getInt("event_id"));
                es.setSponsorId(rs.getInt("sponsor_id"));
                associationList.add(es);
            }
            listAssociations.setItems(associationList);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des associations: " + e.getMessage());
        }
    }

    private void clearFields() {
        cboEvenement.setValue(null);
        cboSponsor.setValue(null);
        cboNiveau.setValue(null);
        txtMontant.clear();
        selectedAssociation = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Cellule personnalisée pour afficher une association en carte */
    private static class EventSponsorListCell extends ListCell<EventSponsor> {
        private final java.util.function.Consumer<EventSponsor> onModifier;
        private final java.util.function.Consumer<EventSponsor> onSupprimer;

        EventSponsorListCell(java.util.function.Consumer<EventSponsor> onModifier, java.util.function.Consumer<EventSponsor> onSupprimer) {
            this.onModifier = onModifier;
            this.onSupprimer = onSupprimer;
        }

        @Override
        protected void updateItem(EventSponsor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox card = new HBox(15);
            card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 6;");
            card.getStyleClass().add("sponsor-card");

            Label lblId = new Label("ID: " + item.getId());
            Label lblEvent = new Label("Événement: " + (item.getNomEvenement() != null ? item.getNomEvenement() : "-"));
            Label lblSponsor = new Label("Sponsor: " + (item.getNomSponsor() != null ? item.getNomSponsor() : "-"));
            Label lblNiveau = new Label("Niveau: " + (item.getNiveau() != null ? item.getNiveau() : "-"));
            Label lblMontant = new Label("Montant: " + String.format("%.2f DT", item.getMontant()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnMod = new Button("Modifier");
            btnMod.getStyleClass().add("btn-modify");
            btnMod.setOnAction(e -> onModifier.accept(item));

            Button btnDel = new Button("Supprimer");
            btnDel.getStyleClass().add("btn-delete");
            btnDel.setOnAction(e -> onSupprimer.accept(item));

            card.getChildren().addAll(lblId, lblEvent, lblSponsor, lblNiveau, lblMontant, spacer, btnMod, btnDel);
            setGraphic(card);
        }
    }
}