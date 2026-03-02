package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.Sponsor;
import utils.DatabaseConnection;
import utils.EmailService;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SponsorViewController implements Initializable {

    @FXML private ListView<Sponsor> listSponsors;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cboFiltreStatut;
    @FXML private ComboBox<String> cboTri;
    @FXML private Label lblCount;
    @FXML private VBox boxDetail;
    @FXML private ImageView imgDetailLogo;
    @FXML private Label lblDetailNom;
    @FXML private Label lblDetailTel;
    @FXML private Label lblDetailEmail;
    @FXML private Label lblDetailStatut;
    @FXML private Label lblPlaceholder;

    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+216\\d{8}$");
    private Sponsor selectedSponsor = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listSponsors.setCellFactory(lv -> new SponsorListCellCompact());

        cboFiltreStatut.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
        cboFiltreStatut.getSelectionModel().select("Tous");
        cboTri.setItems(FXCollections.observableArrayList("Nom (A-Z)", "Nom (Z-A)", "Email (A-Z)", "ID"));
        cboTri.getSelectionModel().select("Nom (A-Z)");

        txtRecherche.textProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboFiltreStatut.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboTri.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());

        loadSponsors();

        listSponsors.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedSponsor = newSelection;
            updateDetailPanel(newSelection);
        });
    }

    private void updateDetailPanel(Sponsor s) {
        if (s == null) {
            if (lblPlaceholder != null) lblPlaceholder.setVisible(true);
            if (boxDetail != null) boxDetail.setVisible(false);
            return;
        }
        if (lblPlaceholder != null) lblPlaceholder.setVisible(false);
        if (boxDetail != null) boxDetail.setVisible(true);
        if (lblDetailNom != null) lblDetailNom.setText("Nom: " + s.getNom());
        if (lblDetailTel != null) lblDetailTel.setText("Tél: " + (s.getTelephone() != null ? s.getTelephone() : "-"));
        if (lblDetailEmail != null) lblDetailEmail.setText("Email: " + (s.getEmail() != null ? s.getEmail() : "-"));
        if (lblDetailStatut != null) {
            lblDetailStatut.setText(s.isStatut() ? "✓ Actif" : "Inactif");
            lblDetailStatut.setStyle(s.isStatut() ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
        }
        if (imgDetailLogo != null) {
            if (s.getLogo() != null && !s.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", s.getLogo());
                    if (Files.exists(logoPath)) {
                        imgDetailLogo.setImage(new Image(logoPath.toUri().toString()));
                    } else imgDetailLogo.setImage(null);
                } catch (Exception e) { imgDetailLogo.setImage(null); }
            } else imgDetailLogo.setImage(null);
        }
    }

    @FXML
    private void ouvrirFormulaireAjout() {
        showSponsorFormDialog(null);
    }

    @FXML
    private void ouvrirFormulaireModification() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Sélectionnez un sponsor à modifier.");
            return;
        }
        showSponsorFormDialog(selectedSponsor);
    }

    private void showSponsorFormDialog(Sponsor toEdit) {
        boolean isModify = (toEdit != null);

        Dialog<Sponsor> dialog = new Dialog<>();
        dialog.setTitle(isModify ? "Modifier le sponsor" : "Nouveau sponsor");
        dialog.setHeaderText(isModify ? "Modifiez les informations du sponsor." : "Créez un nouveau sponsor. (Champs obligatoires: Nom, Téléphone, Email)");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-form-dark");

        ButtonType enregistrerType = new ButtonType("ENREGISTRER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enregistrerType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        Label sectId = new Label("IDENTIFICATION");
        sectId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2471a3; -fx-font-size: 12px;");
        grid.add(sectId, 0, 0, 2, 1);

        Label lblNom = new Label("Nom:");
        TextField txtNom = new TextField();
        txtNom.setPromptText("Nom du sponsor...");
        txtNom.setPrefWidth(280);
        if (toEdit != null) txtNom.setText(toEdit.getNom());
        grid.add(lblNom, 0, 1);
        grid.add(txtNom, 1, 1);

        Label lblTel = new Label("Téléphone:");
        TextField txtTelephone = new TextField();
        txtTelephone.setPromptText("+21612345678");
        if (toEdit != null) txtTelephone.setText(toEdit.getTelephone());
        grid.add(lblTel, 0, 2);
        grid.add(txtTelephone, 1, 2);

        Label lblEmail = new Label("Email:");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("contact@exemple.com");
        if (toEdit != null) txtEmail.setText(toEdit.getEmail());
        grid.add(lblEmail, 0, 3);
        grid.add(txtEmail, 1, 3);

        Label sectLogo = new Label("LOGO");
        sectLogo.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-font-size: 12px;");
        grid.add(sectLogo, 0, 4, 2, 1);

        Label lblLogo = new Label("Image:");
        TextField txtLogo = new TextField();
        txtLogo.setPromptText("Choisir une image...");
        txtLogo.setEditable(false);
        txtLogo.setPrefWidth(200);
        if (toEdit != null && toEdit.getLogo() != null) txtLogo.setText(toEdit.getLogo());
        ImageView imgPreview = new ImageView();
        imgPreview.setFitHeight(40);
        imgPreview.setFitWidth(40);
        imgPreview.setPreserveRatio(true);
        HBox logoBox = new HBox(10);
        logoBox.getChildren().addAll(txtLogo);
        Button btnParcourir = new Button("Parcourir...");
        btnParcourir.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"));
            Window win = dialog.getDialogPane().getScene().getWindow();
            java.io.File file = fc.showOpenDialog(win);
            if (file != null) {
                try {
                    Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "sponsors");
                    Files.createDirectories(uploadsDir);
                    String filename = file.getName();
                    Files.copy(file.toPath(), uploadsDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                    txtLogo.setText(filename);
                    imgPreview.setImage(new Image(file.toURI().toString()));
                } catch (IOException ex) {
                    showAlert("Erreur", "Impossible de copier l'image.");
                }
            }
        });
        logoBox.getChildren().add(btnParcourir);
        logoBox.getChildren().add(imgPreview);
        if (toEdit != null && toEdit.getLogo() != null && !toEdit.getLogo().isBlank()) {
            try {
                Path p = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", toEdit.getLogo());
                if (Files.exists(p)) imgPreview.setImage(new Image(p.toUri().toString()));
            } catch (Exception ignored) {}
        }
        grid.add(lblLogo, 0, 5);
        grid.add(logoBox, 1, 5);

        CheckBox chkStatut = new CheckBox("Sponsor actif");
        chkStatut.setVisible(isModify);
        if (toEdit != null) chkStatut.setSelected(toEdit.isStatut());
        grid.add(chkStatut, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != enregistrerType) return null;
            String nom = txtNom.getText().trim();
            String telephone = txtTelephone.getText().trim();
            String email = txtEmail.getText().trim();
            String logo = txtLogo.getText().trim();
            if (nom.isEmpty() || telephone.isEmpty() || email.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir Nom, Téléphone et Email.");
                return null;
            }
            String err = validerSaisie(nom, telephone, email);
            if (err != null) {
                showAlert("Erreur de saisie", err);
                return null;
            }
            boolean statut = isModify ? chkStatut.isSelected() : true;
            return new Sponsor(toEdit != null ? toEdit.getId() : 0, nom, telephone, email, logo.isEmpty() ? null : logo, statut);
        });

        Optional<Sponsor> result = dialog.showAndWait();
        result.ifPresent(s -> {
            if (isModify) {
                updateSponsor(s);
            } else {
                insertSponsor(s);
            }
        });
    }

    private void insertSponsor(Sponsor s) {
        String query = "INSERT INTO Sponsor (nom, telephone, email, logo, statut) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, s.getNom());
            pstmt.setString(2, s.getTelephone());
            pstmt.setString(3, s.getEmail());
            pstmt.setString(4, s.getLogo());
            pstmt.setBoolean(5, s.isStatut());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                Sponsor nouvelSponsor = new Sponsor(newId, s.getNom(), s.getTelephone(), s.getEmail(), s.getLogo(), s.isStatut());
                sponsorList.add(nouvelSponsor);
                applyFiltresEtTri();

                // Planifier la désactivation automatique si pas associé à au moins 3 événements après 1 minute
                planifierDesactivationSiPeuAssocies(newId);
            }
            showAlert("Succès", "Sponsor ajouté avec succès.");

            // Envoi d'un e-mail de bienvenue (non bloquant si la clé n'est pas configurée)
            if (s.getEmail() != null && !s.getEmail().isBlank()) {
                EmailService.sendWelcomeEmail(s.getEmail(), s.getNom());
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry"))
                showAlert("Erreur", "Cet email existe déjà.");
            else showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    /**
     * Après 1 minute, si le sponsor n'est pas associé à au moins 3 événements,
     * son statut passe automatiquement à inactif.
     */
    private void planifierDesactivationSiPeuAssocies(int sponsorId) {
        scheduler.schedule(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                int nbAssoc = 0;
                try (PreparedStatement countStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM EventSponsor WHERE sponsor_id = ?")) {
                    countStmt.setInt(1, sponsorId);
                    ResultSet rs = countStmt.executeQuery();
                    if (rs.next()) {
                        nbAssoc = rs.getInt(1);
                    }
                }

                if (nbAssoc < 3) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE Sponsor SET statut = false WHERE id = ?")) {
                        updateStmt.setInt(1, sponsorId);
                        updateStmt.executeUpdate();
                    }

                    // Mettre à jour la liste et le panneau de détail sur le thread JavaFX
                    final int idToUpdate = sponsorId;
                    Platform.runLater(() -> {
                        for (Sponsor sp : sponsorList) {
                            if (sp.getId() == idToUpdate) {
                                sp.setStatut(false);
                                break;
                            }
                        }
                        applyFiltresEtTri();
                        if (selectedSponsor != null && selectedSponsor.getId() == idToUpdate) {
                            selectedSponsor.setStatut(false);
                            updateDetailPanel(selectedSponsor);
                        }
                    });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }, 1, TimeUnit.MINUTES);
    }

    private void updateSponsor(Sponsor s) {
        String query = "UPDATE Sponsor SET nom = ?, telephone = ?, email = ?, logo = ?, statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, s.getNom());
            pstmt.setString(2, s.getTelephone());
            pstmt.setString(3, s.getEmail());
            pstmt.setString(4, s.getLogo());
            pstmt.setBoolean(5, s.isStatut());
            pstmt.setInt(6, s.getId());
            pstmt.executeUpdate();
            if (selectedSponsor != null && selectedSponsor.getId() == s.getId()) {
                selectedSponsor.setNom(s.getNom());
                selectedSponsor.setTelephone(s.getTelephone());
                selectedSponsor.setEmail(s.getEmail());
                selectedSponsor.setLogo(s.getLogo());
                selectedSponsor.setStatut(s.isStatut());
                updateDetailPanel(selectedSponsor);
            }
            applyFiltresEtTri();
            showAlert("Succès", "Sponsor modifié avec succès.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerSponsor() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Sélectionnez un sponsor à supprimer.");
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
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Sponsor WHERE id = ?")) {
            pstmt.setInt(1, selectedSponsor.getId());
            pstmt.executeUpdate();
            sponsorList.remove(selectedSponsor);
            selectedSponsor = null;
            updateDetailPanel(null);
            listSponsors.getSelectionModel().clearSelection();
            applyFiltresEtTri();
            showAlert("Succès", "Sponsor supprimé avec succès.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
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

    private void loadSponsors() {
        sponsorList.clear();
        String query = "SELECT * FROM Sponsor ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                sponsorList.add(new Sponsor(rs.getInt("id"), rs.getString("nom"), rs.getString("telephone"),
                        rs.getString("email"), rs.getString("logo"), rs.getBoolean("statut")));
            }
            applyFiltresEtTri();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void applyFiltresEtTri() {
        String search = txtRecherche != null ? txtRecherche.getText().trim().toLowerCase() : "";
        String filtreStatut = (cboFiltreStatut != null && cboFiltreStatut.getValue() != null) ? cboFiltreStatut.getValue() : "Tous";
        String tri = (cboTri != null && cboTri.getValue() != null) ? cboTri.getValue() : "Nom (A-Z)";

        Stream<Sponsor> stream = sponsorList.stream()
                .filter(s -> search.isEmpty() || Stream.of(s.getNom(), s.getEmail() != null ? s.getEmail() : "", s.getTelephone() != null ? s.getTelephone() : "").anyMatch(v -> v.toLowerCase().contains(search)))
                .filter(s -> "Tous".equals(filtreStatut) || ("Actifs".equals(filtreStatut) && s.isStatut()) || ("Inactifs".equals(filtreStatut) && !s.isStatut()));

        Comparator<Sponsor> comparator = switch (tri) {
            case "Nom (Z-A)" -> Comparator.comparing(Sponsor::getNom, Comparator.reverseOrder());
            case "Email (A-Z)" -> Comparator.comparing(s2 -> s2.getEmail() != null ? s2.getEmail() : "");
            case "ID" -> Comparator.comparingInt(Sponsor::getId);
            default -> Comparator.comparing(Sponsor::getNom, String.CASE_INSENSITIVE_ORDER);
        };
        List<Sponsor> filtered = stream.sorted(comparator).collect(Collectors.toList());
        listSponsors.setItems(FXCollections.observableArrayList(filtered));
        if (lblCount != null) lblCount.setText(filtered.size() + " sponsors");
    }

    private boolean hasAssociatedEvents(int sponsorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM EventSponsor WHERE sponsor_id = ?")) {
            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private String validerSaisie(String nom, String telephone, String email) {
        if (nom == null || nom.trim().length() < 2) return "Le nom doit contenir au moins 2 caractères.";
        if (!EMAIL_PATTERN.matcher(email).matches()) return "Format email invalide (ex: contact@exemple.com)";
        String tel = telephone.replaceAll("\\s", "").trim();
        if (!PHONE_PATTERN.matcher(tel).matches()) return "Téléphone invalide. Format: +216 suivi de 8 chiffres.";
        return null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class SponsorListCellCompact extends ListCell<Sponsor> {
        @Override
        protected void updateItem(Sponsor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView img = new ImageView();
            img.setFitHeight(32);
            img.setFitWidth(32);
            img.setPreserveRatio(true);
            if (item.getLogo() != null && !item.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", item.getLogo());
                    if (Files.exists(logoPath)) img.setImage(new Image(logoPath.toUri().toString()));
                } catch (Exception ignored) {}
            }
            Label name = new Label(item.getNom());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");
            Label statut = new Label(item.isStatut() ? "ACTIF" : "Inactif");
            statut.setStyle(item.isStatut() ? "-fx-text-fill: #27ae60; -fx-font-size: 11px;" : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            row.getChildren().addAll(img, name, statut);
            setGraphic(row);
        }
    }
}
