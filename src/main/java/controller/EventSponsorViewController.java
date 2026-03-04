package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.EventSponsor;
import model.Evenement;
import model.Sponsor;
import utils.DatabaseConnection;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class EventSponsorViewController implements Initializable {

    @FXML private ListView<EventSponsor> listAssociations;
    @FXML private Label lblCount;
    @FXML private VBox boxDetail;
    @FXML private Label lblDetailEvent;
    @FXML private Label lblDetailSponsor;
    @FXML private Label lblDetailNiveau;
    @FXML private Label lblDetailMontant;
    @FXML private Label lblPlaceholder;

    private ObservableList<Evenement> evenementList = FXCollections.observableArrayList();
    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private ObservableList<EventSponsor> associationList = FXCollections.observableArrayList();
    private EventSponsor selectedAssociation = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listAssociations.setCellFactory(lv -> new EventSponsorListCellCompact());
        loadEvenements();
        loadSponsors();
        loadAssociations();
        listAssociations.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selectedAssociation = n;
            updateDetailPanel(n);
        });
    }

    private void updateDetailPanel(EventSponsor es) {
        if (es == null) {
            if (lblPlaceholder != null) lblPlaceholder.setVisible(true);
            if (boxDetail != null) boxDetail.setVisible(false);
            return;
        }
        if (lblPlaceholder != null) lblPlaceholder.setVisible(false);
        if (boxDetail != null) boxDetail.setVisible(true);
        if (lblDetailEvent != null) lblDetailEvent.setText("Événement: " + (es.getNomEvenement() != null ? es.getNomEvenement() : "-"));
        if (lblDetailSponsor != null) lblDetailSponsor.setText("Sponsor: " + (es.getNomSponsor() != null ? es.getNomSponsor() : "-"));
        if (lblDetailNiveau != null) lblDetailNiveau.setText("Niveau: " + (es.getNiveau() != null ? es.getNiveau() : "-"));
        if (lblDetailMontant != null) lblDetailMontant.setText("Montant: " + String.format("%.2f DT", es.getMontant()));
    }

    @FXML
    private void ouvrirFormulaireAjout() {
        showAssociationFormDialog(null);
    }

    @FXML
    private void ouvrirFormulaireModification() {
        if (selectedAssociation == null) {
            showAlert("Erreur", "Sélectionnez une association à modifier.");
            return;
        }
        showAssociationFormDialog(selectedAssociation);
    }

    private void showAssociationFormDialog(EventSponsor toEdit) {
        boolean isModify = (toEdit != null);
        Dialog<EventSponsor> dialog = new Dialog<>();
        dialog.setTitle(isModify ? "Modifier l'association" : "Nouvelle association");
        dialog.setHeaderText(isModify ? "Modifiez l'association." : "Associez un événement et un sponsor.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-form-dark");

        ButtonType enregistrerType = new ButtonType("ENREGISTRER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enregistrerType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        Label sect = new Label("COORDONNÉES");
        sect.setStyle("-fx-font-weight: bold; -fx-text-fill: #2471a3; -fx-font-size: 12px;");
        grid.add(sect, 0, 0, 2, 1);

        ComboBox<Evenement> cboEvenement = new ComboBox<>(evenementList);
        cboEvenement.setPromptText("Choisir événement");
        cboEvenement.setPrefWidth(280);
        if (toEdit != null) evenementList.stream().filter(e -> e.getId() == toEdit.getEventId()).findFirst().ifPresent(cboEvenement::setValue);
        grid.add(new Label("Événement:"), 0, 1);
        grid.add(cboEvenement, 1, 1);

        ComboBox<Sponsor> cboSponsor = new ComboBox<>(sponsorList);
        cboSponsor.setPromptText("Choisir sponsor");
        cboSponsor.setPrefWidth(280);
        if (toEdit != null) sponsorList.stream().filter(s -> s.getId() == toEdit.getSponsorId()).findFirst().ifPresent(cboSponsor::setValue);
        grid.add(new Label("Sponsor:"), 0, 2);
        grid.add(cboSponsor, 1, 2);

        ComboBox<String> cboNiveau = new ComboBox<>(FXCollections.observableArrayList("GOLD", "SILVER", "BRONZE", "PARTENAIRE"));
        cboNiveau.setPromptText("Niveau");
        cboNiveau.setPrefWidth(120);
        if (toEdit != null) cboNiveau.setValue(toEdit.getNiveau());
        grid.add(new Label("Niveau:"), 0, 3);
        grid.add(cboNiveau, 1, 3);

        TextField txtMontant = new TextField();
        txtMontant.setPromptText("0.00");
        txtMontant.setPrefWidth(120);
        if (toEdit != null) txtMontant.setText(String.format("%.2f", toEdit.getMontant()));
        grid.add(new Label("Montant (DT):"), 0, 4);
        grid.add(txtMontant, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != enregistrerType) return null;
            Evenement ev = cboEvenement.getValue();
            Sponsor sp = cboSponsor.getValue();
            String niveau = cboNiveau.getValue();
            String montantStr = txtMontant.getText().trim();
            if (ev == null || sp == null || niveau == null || montantStr.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs.");
                return null;
            }
            double montant;
            try {
                montant = Double.parseDouble(montantStr);
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le montant doit être un nombre valide.");
                return null;
            }
            EventSponsor es = new EventSponsor(toEdit != null ? toEdit.getId() : 0, ev.getNom(), sp.getNom(), niveau, montant);
            es.setEventId(ev.getId());
            es.setSponsorId(sp.getId());
            return es;
        });

        Optional<EventSponsor> result = dialog.showAndWait();
        result.ifPresent(es -> {
            if (isModify) {
                updateAssociation(es);
            } else {
                insertAssociation(es);
            }
        });
    }

    private void insertAssociation(EventSponsor es) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO EventSponsor (event_id, sponsor_id, niveau, montant) VALUES (?, ?, ?, ?)")) {
            pstmt.setInt(1, es.getEventId());
            pstmt.setInt(2, es.getSponsorId());
            pstmt.setString(3, es.getNiveau());
            pstmt.setDouble(4, es.getMontant());
            pstmt.executeUpdate();
            loadAssociations();
            showAlert("Succès", "Association créée avec succès.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry"))
                showAlert("Erreur", "Ce sponsor est déjà associé à cet événement.");
            else showAlert("Erreur", "Erreur: " + e.getMessage());
        }
    }

    private void updateAssociation(EventSponsor es) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE EventSponsor SET event_id = ?, sponsor_id = ?, niveau = ?, montant = ? WHERE id = ?")) {
            pstmt.setInt(1, es.getEventId());
            pstmt.setInt(2, es.getSponsorId());
            pstmt.setString(3, es.getNiveau());
            pstmt.setDouble(4, es.getMontant());
            pstmt.setInt(5, es.getId());
            pstmt.executeUpdate();
            loadAssociations();
            selectedAssociation = null;
            listAssociations.getSelectionModel().clearSelection();
            updateDetailPanel(null);
            showAlert("Succès", "Association modifiée avec succès.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry"))
                showAlert("Erreur", "Ce sponsor est déjà associé à cet événement.");
            else showAlert("Erreur", "Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerAssociation() {
        if (selectedAssociation == null) {
            showAlert("Erreur", "Sélectionnez une association à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'association");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette association ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM EventSponsor WHERE id = ?")) {
            pstmt.setInt(1, selectedAssociation.getId());
            pstmt.executeUpdate();
            associationList.remove(selectedAssociation);
            selectedAssociation = null;
            updateDetailPanel(null);
            listAssociations.getSelectionModel().clearSelection();
            loadAssociations();
            showAlert("Succès", "Association supprimée avec succès.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @FXML
    private void actualiser() {
        loadEvenements();
        loadSponsors();
        loadAssociations();
        selectedAssociation = null;
        updateDetailPanel(null);
        listAssociations.getSelectionModel().clearSelection();
    }

    @FXML
    private void exporterPdf() {
        if (associationList.isEmpty()) {
            showAlert("Information", "Aucune association à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la liste en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        chooser.setInitialFileName("associations-event-sponsor.pdf");
        File file = chooser.showSaveDialog(listAssociations.getScene().getWindow());
        if (file == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 40;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float tableWidth = pageWidth - 2 * margin;

            float[] colWidths = new float[]{
                    tableWidth * 0.40f, // Événement
                    tableWidth * 0.30f, // Sponsor
                    tableWidth * 0.15f, // Niveau
                    tableWidth * 0.15f  // Montant
            };

            float rowHeight = 20f;

            PDPageContentStream content = new PDPageContentStream(document, page);

            // Titre
            float y = pageHeight - 60;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.newLineAtOffset(margin, y);
            content.showText("Liste des associations événement - sponsor");
            content.endText();

            y -= 30;

            // Dessine l'en-tête du tableau pour la première page
            drawPdfHeaderRow(content, y, margin, rowHeight, tableWidth, colWidths);
            y -= rowHeight;

            int index = 0;
            for (EventSponsor es : associationList) {
                if (y < margin + rowHeight * 2) {
                    // Nouvelle page
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - 60;
                    drawPdfHeaderRow(content, y, margin, rowHeight, tableWidth, colWidths);
                    y -= rowHeight;
                }

                float x = margin;

                // Ligne alternée (zébrage)
                if (index % 2 == 0) {
                    content.setNonStrokingColor(248, 249, 250);
                    content.addRect(margin, y - rowHeight, tableWidth, rowHeight);
                    content.fill();
                }

                // Bordure de la ligne
                content.setStrokingColor(200, 200, 200);
                content.addRect(margin, y - rowHeight, tableWidth, rowHeight);
                content.stroke();

                // Texte de la ligne
                content.setNonStrokingColor(33, 37, 41);
                float textY = y - 14;

                String eventName = es.getNomEvenement() != null ? es.getNomEvenement() : "-";
                String sponsorName = es.getNomSponsor() != null ? es.getNomSponsor() : "-";
                String niveau = es.getNiveau() != null ? es.getNiveau() : "-";
                String montant = String.format("%.2f", es.getMontant());

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(eventName);
                content.endText();

                x += colWidths[0];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(sponsorName);
                content.endText();

                x += colWidths[1];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(niveau);
                content.endText();

                x += colWidths[2];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(montant);
                content.endText();

                y -= rowHeight;
                index++;
            }

            content.close();
            document.save(file);
            showAlert("Succès", "PDF généré avec succès : " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    private void drawPdfHeaderRow(PDPageContentStream content,
                                  float headerY,
                                  float margin,
                                  float rowHeight,
                                  float tableWidth,
                                  float[] colWidths) throws java.io.IOException {
        float x = margin;

        // Fond de l'en-tête
        content.setNonStrokingColor(44, 62, 80); // bleu foncé
        content.addRect(margin, headerY - rowHeight, tableWidth, rowHeight);
        content.fill();

        // Bordure
        content.setStrokingColor(30, 39, 46);
        content.addRect(margin, headerY - rowHeight, tableWidth, rowHeight);
        content.stroke();

        // Texte de l'en-tête
        content.setNonStrokingColor(255, 255, 255);
        float textY = headerY - 14;

        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Événement");
        content.endText();

        x += colWidths[0];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Sponsor");
        content.endText();

        x += colWidths[1];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Niveau");
        content.endText();

        x += colWidths[2];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Montant (DT)");
        content.endText();
    }

    private void loadEvenements() {
        evenementList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM evenement ORDER BY date_debut DESC")) {
            while (rs.next()) {
                evenementList.add(new Evenement(rs.getInt("id_event"), rs.getString("titre"),
                        rs.getDate("date_debut").toLocalDate(), rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"), rs.getString("description"), rs.getString("statut")));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement événements: " + e.getMessage());
        }
    }

    private void loadSponsors() {
        sponsorList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Sponsor WHERE statut = true ORDER BY nom")) {
            while (rs.next()) {
                sponsorList.add(new Sponsor(rs.getInt("id"), rs.getString("nom"), rs.getString("telephone"),
                        rs.getString("email"), rs.getString("logo"), rs.getBoolean("statut")));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement sponsors: " + e.getMessage());
        }
    }

    private void loadAssociations() {
        associationList.clear();
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "ORDER BY e.date_debut DESC, es.niveau";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                EventSponsor es = new EventSponsor(rs.getInt("id"), rs.getString("event_nom"), rs.getString("sponsor_nom"), rs.getString("niveau"), rs.getDouble("montant"));
                es.setEventId(rs.getInt("event_id"));
                es.setSponsorId(rs.getInt("sponsor_id"));
                associationList.add(es);
            }
            listAssociations.setItems(associationList);
            if (lblCount != null) lblCount.setText(associationList.size() + " associations");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement associations: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class EventSponsorListCellCompact extends ListCell<EventSponsor> {
        @Override
        protected void updateItem(EventSponsor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label ev = new Label(item.getNomEvenement() != null ? item.getNomEvenement() : "-");
            ev.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");
            Label sp = new Label(" • " + (item.getNomSponsor() != null ? item.getNomSponsor() : "-"));
            sp.setStyle("-fx-text-fill: #ecf0f1;");
            Label niv = new Label(" • " + (item.getNiveau() != null ? item.getNiveau() : "-"));
            niv.setStyle("-fx-text-fill: #ecf0f1;");
            row.getChildren().addAll(ev, sp, niv);
            setGraphic(row);
        }
    }
}
