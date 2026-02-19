package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import model.Sponsor;
import utils.DatabaseConnection;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.stage.FileChooser;
import javafx.stage.Window;

public class SponsorViewController implements Initializable {

    @FXML
    private TextField txtNom;
    @FXML
    private TextField txtTelephone;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtLogo;
    @FXML
    private ImageView imgLogoPreview;
    @FXML
    private CheckBox chkStatut;
    @FXML
    private ListView<Sponsor> listSponsors;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cboFiltreStatut;
    @FXML
    private ComboBox<String> cboTri;
    @FXML
    private Label lblCount;

    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+216\\d{8}$");
    private Sponsor selectedSponsor = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listSponsors.setCellFactory(lv -> new SponsorListCell(this::onModifierClick, this::onSupprimerClick));

        cboFiltreStatut.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
        cboFiltreStatut.getSelectionModel().select("Tous");

        cboTri.setItems(FXCollections.observableArrayList("Nom (A-Z)", "Nom (Z-A)", "Email (A-Z)", "ID"));
        cboTri.getSelectionModel().select("Nom (A-Z)");

        txtRecherche.textProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboFiltreStatut.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboTri.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());

        loadSponsors();

        listSponsors.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedSponsor = newSelection;
                txtNom.setText(newSelection.getNom());
                txtTelephone.setText(newSelection.getTelephone());
                txtEmail.setText(newSelection.getEmail());
                txtLogo.setText(newSelection.getLogo() != null ? newSelection.getLogo() : "");
                chkStatut.setSelected(newSelection.isStatut());
                updateLogoPreview(newSelection.getLogo());
            }
        });
    }

    private void updateLogoPreview(String filename) {
        if (imgLogoPreview == null) return;
        if (filename == null || filename.isBlank()) {
            imgLogoPreview.setImage(null);
            return;
        }
        try {
            Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", filename);
            if (Files.exists(logoPath)) {
                imgLogoPreview.setImage(new Image(logoPath.toUri().toString()));
            } else {
                imgLogoPreview.setImage(null);
            }
        } catch (Exception e) {
            imgLogoPreview.setImage(null);
        }
    }

    @FXML
    private void choisirLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );
        Window win = txtLogo.getScene().getWindow();
        java.io.File file = fc.showOpenDialog(win);
        if (file != null) {
            try {
                Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "sponsors");
                Files.createDirectories(uploadsDir);
                String filename = file.getName();
                Path dest = uploadsDir.resolve(filename);
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                txtLogo.setText(filename);
                updateLogoPreview(filename);
            } catch (IOException e) {
                showAlert("Erreur", "Impossible de copier l'image: " + e.getMessage());
            }
        }
    }

    private void onModifierClick(Sponsor s) {
        listSponsors.getSelectionModel().select(s);
        selectedSponsor = s;
        txtNom.setText(s.getNom());
        txtTelephone.setText(s.getTelephone());
        txtEmail.setText(s.getEmail());
        txtLogo.setText(s.getLogo() != null ? s.getLogo() : "");
        chkStatut.setSelected(s.isStatut());
        updateLogoPreview(s.getLogo());
    }

    private void onSupprimerClick(Sponsor s) {
        selectedSponsor = s;
        supprimerSponsor();
    }

    @FXML
    private void ajouterSponsor() {
        String nom = txtNom.getText().trim();
        String telephone = txtTelephone.getText().trim();
        String email = txtEmail.getText().trim();
        String logo = txtLogo.getText().trim();
        boolean statut = chkStatut.isSelected();

        if (nom.isEmpty() || telephone.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires (Nom, Téléphone, Email)");
            return;
        }
        String validationError = validerSaisie(nom, telephone, email);
        if (validationError != null) {
            showAlert("Erreur de saisie", validationError);
            return;
        }

        String query = "INSERT INTO Sponsor (nom, telephone, email, logo, statut) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nom);
            pstmt.setString(2, telephone);
            pstmt.setString(3, email);
            pstmt.setString(4, logo.isEmpty() ? null : logo);
            pstmt.setBoolean(5, statut);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                sponsorList.add(new Sponsor(rs.getInt(1), nom, telephone, email, logo, statut));
                applyFiltresEtTri();
            }

            clearFields();
            showAlert("Succès", "Sponsor ajouté avec succès");

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                showAlert("Erreur", "Cet email existe déjà");
            } else {
                showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void modifierSponsor() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Veuillez sélectionner un sponsor à modifier");
            return;
        }

        String nom = txtNom.getText().trim();
        String telephone = txtTelephone.getText().trim();
        String email = txtEmail.getText().trim();
        String logo = txtLogo.getText().trim();
        boolean statut = chkStatut.isSelected();

        if (nom.isEmpty() || telephone.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
            return;
        }
        String validationError = validerSaisie(nom, telephone, email);
        if (validationError != null) {
            showAlert("Erreur de saisie", validationError);
            return;
        }

        String query = "UPDATE Sponsor SET nom = ?, telephone = ?, email = ?, logo = ?, statut = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, nom);
            pstmt.setString(2, telephone);
            pstmt.setString(3, email);
            pstmt.setString(4, logo.isEmpty() ? null : logo);
            pstmt.setBoolean(5, statut);
            pstmt.setInt(6, selectedSponsor.getId());
            pstmt.executeUpdate();

            selectedSponsor.setNom(nom);
            selectedSponsor.setTelephone(telephone);
            selectedSponsor.setEmail(email);
            selectedSponsor.setLogo(logo);
            selectedSponsor.setStatut(statut);
            applyFiltresEtTri();

            clearFields();
            selectedSponsor = null;
            showAlert("Succès", "Sponsor modifié avec succès");

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerSponsor() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Veuillez sélectionner un sponsor à supprimer");
            return;
        }

        if (hasAssociatedEvents(selectedSponsor.getId())) {
            showAlert("Attention", "Ce sponsor est associé à des événements. Supprimez d'abord ces associations.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le sponsor");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce sponsor ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM Sponsor WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, selectedSponsor.getId());
                pstmt.executeUpdate();

                sponsorList.remove(selectedSponsor);
                applyFiltresEtTri();
                clearFields();
                selectedSponsor = null;
                showAlert("Succès", "Sponsor supprimé avec succès");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private void loadSponsors() {
        sponsorList.clear();
        String query = "SELECT * FROM Sponsor ORDER BY nom";

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
            applyFiltresEtTri();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    /** Filtrage, recherche et tri avec les streams */
    private void applyFiltresEtTri() {
        String search = txtRecherche != null ? txtRecherche.getText().trim().toLowerCase() : "";
        String filtreStatut = cboFiltreStatut != null && cboFiltreStatut.getValue() != null ? cboFiltreStatut.getValue() : "Tous";
        String tri = cboTri != null && cboTri.getValue() != null ? cboTri.getValue() : "Nom (A-Z)";

        Stream<Sponsor> stream = sponsorList.stream()
                .filter(s -> search.isEmpty() || Stream.of(
                        s.getNom(),
                        s.getEmail() != null ? s.getEmail() : "",
                        s.getTelephone() != null ? s.getTelephone() : ""
                ).anyMatch(v -> v.toLowerCase().contains(search)))
                .filter(s -> "Tous".equals(filtreStatut)
                        || ("Actifs".equals(filtreStatut) && s.isStatut())
                        || ("Inactifs".equals(filtreStatut) && !s.isStatut()));

        Comparator<Sponsor> comparator = switch (tri) {
            case "Nom (Z-A)" -> Comparator.comparing(Sponsor::getNom, Comparator.reverseOrder());
            case "Email (A-Z)" -> Comparator.comparing(s -> s.getEmail() != null ? s.getEmail() : "");
            case "ID" -> Comparator.comparingInt(Sponsor::getId);
            default -> Comparator.comparing(Sponsor::getNom, String.CASE_INSENSITIVE_ORDER);
        };

        List<Sponsor> filtered = stream.sorted(comparator).collect(Collectors.toList());
        listSponsors.setItems(FXCollections.observableArrayList(filtered));
        if (lblCount != null) {
            lblCount.setText(filtered.size() + " sponsor(s)");
        }
    }

    @FXML
    private void actualiser() {
        loadSponsors();
    }

    @FXML
    private void reinitialiserFiltres() {
        if (txtRecherche != null) txtRecherche.clear();
        if (cboFiltreStatut != null) cboFiltreStatut.getSelectionModel().select("Tous");
        if (cboTri != null) cboTri.getSelectionModel().select("Nom (A-Z)");
        applyFiltresEtTri();
    }

    private boolean hasAssociatedEvents(int sponsorId) {
        String query = "SELECT COUNT(*) FROM EventSponsor WHERE sponsor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Contrôle de saisie : validation email et téléphone (format Tunisie: +216 + 8 chiffres) */
    private String validerSaisie(String nom, String telephone, String email) {
        if (nom == null || nom.trim().length() < 2) {
            return "Le nom doit contenir au moins 2 caractères.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Format email invalide (ex: contact@exemple.com)";
        }
        String tel = telephone.replaceAll("\\s", "").trim();
        if (!PHONE_PATTERN.matcher(tel).matches()) {
            return "Téléphone invalide. Format: +216 suivi de 8 chiffres (ex: +21612345678)";
        }
        return null;
    }

    private void clearFields() {
        txtNom.clear();
        txtTelephone.clear();
        txtEmail.clear();
        txtLogo.clear();
        chkStatut.setSelected(true);
        updateLogoPreview(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void annuler() {
        clearFields();
        selectedSponsor = null;
        listSponsors.getSelectionModel().clearSelection();
    }

    /** Cellule personnalisée pour afficher un sponsor en carte */
    private static class SponsorListCell extends ListCell<Sponsor> {
        private final java.util.function.Consumer<Sponsor> onModifier;
        private final java.util.function.Consumer<Sponsor> onSupprimer;

        SponsorListCell(java.util.function.Consumer<Sponsor> onModifier, java.util.function.Consumer<Sponsor> onSupprimer) {
            this.onModifier = onModifier;
            this.onSupprimer = onSupprimer;
        }

        @Override
        protected void updateItem(Sponsor item, boolean empty) {
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
            Label lblNom = new Label("Nom: " + item.getNom());
            Label lblTel = new Label("Tél: " + (item.getTelephone() != null ? item.getTelephone() : "-"));
            Label lblEmail = new Label("Email: " + (item.getEmail() != null ? item.getEmail() : "-"));
            ImageView imgLogo = new ImageView();
            imgLogo.setFitHeight(36);
            imgLogo.setFitWidth(36);
            imgLogo.setPreserveRatio(true);
            if (item.getLogo() != null && !item.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", item.getLogo());
                    if (Files.exists(logoPath)) {
                        imgLogo.setImage(new Image(logoPath.toUri().toString()));
                    }
                } catch (Exception ignored) {}
            }
            Label lblStatut = new Label(item.isStatut() ? "✓ Actif" : "Inactif");
            lblStatut.setStyle(item.isStatut() ? "-fx-text-fill: green;" : "-fx-text-fill: #e74c3c;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnMod = new Button("Modifier");
            btnMod.getStyleClass().add("btn-modify");
            btnMod.setOnAction(e -> onModifier.accept(item));

            Button btnDel = new Button("Supprimer");
            btnDel.getStyleClass().add("btn-delete");
            btnDel.setOnAction(e -> onSupprimer.accept(item));

            card.getChildren().addAll(lblId, lblNom, lblTel, lblEmail, imgLogo, lblStatut, spacer, btnMod, btnDel);
            setGraphic(card);
        }
    }
}