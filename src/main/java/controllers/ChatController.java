package controllers;

import entities.GroupeChat;
import entities.MessageChat;
import Services.BannedWordsService;
import Services.GroupeChatService;
import Services.MessageChatService;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ChatController implements Initializable {

    @FXML private ListView<GroupeChat> groupsList;
    @FXML private Label activeGroupName;
    @FXML private Label groupInfo;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageField;
    @FXML private TextField searchField;
    @FXML private Button btnNewGroup;
    @FXML private Button btnSend;
    @FXML private Button btnMenu;
    @FXML private Button btnAttach;
    @FXML private Button btnLocation;

    private GroupeChatService groupeService;
    private MessageChatService messageService;
    private BannedWordsService bannedWordsService;
    private String pendingFichierPath;
    private String pendingTypeMessage;
    private Double pendingLatitude;
    private Double pendingLongitude;
    private ObservableList<GroupeChat> groupesObservableList;
    private GroupeChat selectedGroupe;
    private Timer refreshTimer;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        groupeService = new GroupeChatService();
        messageService = new MessageChatService();
        bannedWordsService = new BannedWordsService();
        groupesObservableList = FXCollections.observableArrayList();
        pendingTypeMessage = "TEXT";

        // Configuration de la ListView pour afficher les groupes
        groupsList.setCellFactory(param -> new ListCell<GroupeChat>() {
            @Override
            protected void updateItem(GroupeChat groupe, boolean empty) {
                super.updateItem(groupe, empty);
                if (empty || groupe == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(5);
                    vbox.setPadding(new Insets(10, 15, 10, 15));
                    
                    Label nomLabel = new Label(groupe.getNom());
                    nomLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    nomLabel.setTextFill(Color.web("#1a1a1a"));
                    
                    Label descLabel = new Label(groupe.getDescription() != null && !groupe.getDescription().isEmpty() 
                        ? groupe.getDescription() : "Aucune description");
                    descLabel.setFont(Font.font("System", 12));
                    descLabel.setTextFill(Color.web("#666666"));
                    descLabel.setWrapText(true);
                    descLabel.setMaxWidth(300);
                    
                    vbox.getChildren().addAll(nomLabel, descLabel);
                    setGraphic(vbox);
                }
            }
        });

        // Sélection d'un groupe
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedGroupe = newVal;
                activeGroupName.setText(newVal.getNom());
                groupInfo.setText(newVal.getType() + " • Groupe de discussion");
                loadMessages(newVal.getId());
            } else {
                selectedGroupe = null;
                activeGroupName.setText("Sélectionnez un groupe");
                groupInfo.setText("Discutez avec les amis sur les équipements");
                afficherMessageAccueil();
            }
        });

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                loadGroupes();
            } else {
                List<GroupeChat> filtered = groupeService.rechercherParNom(newVal.trim());
                groupesObservableList.clear();
                groupesObservableList.addAll(filtered);
            }
        });

        // Enter = send
        messageField.setOnAction(e -> onSend());

        // Charger les groupes
        loadGroupes();

        // Message d'accueil quand aucun groupe n'est sélectionné
        afficherMessageAccueil();

        // Démarrer le rafraîchissement automatique des messages (temps réel)
        startAutoRefresh();
    }

    private void afficherMessageAccueil() {
        if (messagesBox == null) return;
        messagesBox.getChildren().clear();
        Label welcome = new Label("Sélectionnez un groupe à gauche pour voir les messages et discuter avec les amis sur les équipements.");
        welcome.setWrapText(true);
        welcome.setMaxWidth(500);
        welcome.setFont(Font.font("System", 14));
        welcome.setTextFill(Color.web("#666666"));
        VBox box = new VBox(welcome);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        messagesBox.getChildren().add(box);
    }

    private void loadGroupes() {
        if (groupeService == null || groupsList == null) return;
        
        try {
            List<GroupeChat> groupes = groupeService.afficher();
            if (groupes == null) return;
            
            groupesObservableList.clear();
            groupesObservableList.addAll(groupes);
            groupsList.setItems(groupesObservableList);
        } catch (Exception e) {
            System.err.println("Chargement groupes: " + e.getMessage());
        }
    }

    private void loadMessages(int idGroupe) {
        if (messagesBox == null) return;
        
        messagesBox.getChildren().clear();
        List<MessageChat> messages = messageService.afficherParGroupe(idGroupe);
        
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // Stream : filtrer les messages valides, grouper par jour, trier les jours puis afficher
        List<MessageChat> validMessages = messages.stream()
                .filter(msg -> msg != null && msg.getDateEnvoi() != null)
                .toList();

        validMessages.stream()
                .map(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate())
                .distinct()
                .sorted()
                .forEach(date -> {
                    Timestamp firstOfDay = validMessages.stream()
                            .filter(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate().equals(date))
                            .min(Comparator.comparing(MessageChat::getDateEnvoi, Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(MessageChat::getDateEnvoi)
                            .orElse(null);
                    if (firstOfDay != null) {
                        addDateSeparator(firstOfDay);
                    }
                    validMessages.stream()
                            .filter(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate().equals(date))
                            .sorted(Comparator.comparing(MessageChat::getDateEnvoi, Comparator.nullsLast(Comparator.naturalOrder())))
                            .forEach(this::addMessageBubble);
                });
        
        // Scroll vers le bas
        Platform.runLater(() -> {
            if (messagesScroll != null) {
                messagesScroll.setVvalue(1.0);
            }
        });
    }

    private boolean isSameDay(LocalDateTime date1, Timestamp timestamp) {
        if (date1 == null || timestamp == null) return false;
        LocalDateTime date2 = timestamp.toLocalDateTime();
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    private void addDateSeparator(Timestamp timestamp) {
        Label dateLabel = new Label(timestamp.toLocalDateTime().format(dateFmt));
        dateLabel.getStyleClass().add("date-separator");
        HBox separatorBox = new HBox(dateLabel);
        separatorBox.setAlignment(Pos.CENTER);
        separatorBox.setPadding(new Insets(10, 0, 10, 0));
        messagesBox.getChildren().add(separatorBox);
    }

    private void addMessageBubble(MessageChat msg) {
        if (msg == null || messagesBox == null) return;

        boolean isMe = msg.getId() % 2 == 0;
        String type = msg.getTypeMessage() != null ? msg.getTypeMessage() : "TEXT";
        String contenu = msg.getContenu() != null ? msg.getContenu() : "";
        String path = msg.getFichierPath();
        Double lat = msg.getLatitude();
        Double lon = msg.getLongitude();

        VBox bubble = new VBox(6);
        bubble.getStyleClass().addAll("message-bubble", "message-bubble-admin", isMe ? "bubble-me" : "bubble-other");
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 15, 10, 15));

        // Contenu selon le type : image (aperçu + ouvrir), document (icône + ouvrir), position (carte), texte
        if ("IMAGE".equals(type) && path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    Image img = new Image("file:" + f.getAbsolutePath(), 280, 200, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    iv.setOnMouseClicked(e -> ouvrirFichier(f));
                    bubble.getChildren().add(iv);
                } catch (Exception e) {
                    ajouterLignePieceJointe(bubble, "🖼 " + f.getName(), f);
                }
            } else {
                ajouterLignePieceJointe(bubble, "🖼 " + new File(path).getName() + " (fichier absent)", null);
            }
            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.setTextFill(Color.web("#e2e8f0"));
                bubble.getChildren().add(txt);
            }
            Button btnOuvrir = new Button("Ouvrir l'image");
            btnOuvrir.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
            btnOuvrir.setOnAction(e -> ouvrirFichier(f));
            bubble.getChildren().add(btnOuvrir);
        } else if ("PDF".equals(type) || "AUDIO".equals(type) || "VIDEO".equals(type)) {
            File f = path != null ? new File(path) : null;
            String icon = "PDF".equals(type) ? "📄" : "AUDIO".equals(type) ? "🎵" : "🎬";
            String nom = f != null ? f.getName() : "fichier";
            ajouterLignePieceJointe(bubble, icon + " " + nom, f);
            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.setTextFill(Color.web("#e2e8f0"));
                bubble.getChildren().add(txt);
            }
            if (f != null && f.exists()) {
                Button btnOuvrir = new Button("Ouvrir / Consulter");
                btnOuvrir.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
                btnOuvrir.setOnAction(e -> ouvrirFichier(f));
                bubble.getChildren().add(btnOuvrir);
            }
        } else if ("LOCATION".equals(type) && lat != null && lon != null) {
            Label titre = new Label("📍 Position actuelle");
            titre.setFont(Font.font("System", FontWeight.BOLD, 13));
            titre.setTextFill(Color.web("#e2e8f0"));
            Label coords = new Label(String.format("%.5f, %.5f", lat, lon));
            coords.setFont(Font.font("System", 12));
            coords.setTextFill(Color.web("#94a3b8"));
            Button btnCarte = new Button("Voir sur la carte");
            btnCarte.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
            btnCarte.setOnAction(e -> ouvrirCarteNavigateur(lat, lon));
            bubble.getChildren().addAll(titre, coords, btnCarte);
            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.setTextFill(Color.web("#e2e8f0"));
                bubble.getChildren().add(txt);
            }
        } else {
            if (type != null && !"TEXT".equals(type) && (path != null || (lat != null && lon != null))) {
                if (path != null) contenu = "[" + type + "] " + (contenu.isEmpty() ? new File(path).getName() : contenu);
                else contenu = "📍 Position partagée";
            }
            Label msgLabel = new Label(contenu.isEmpty() ? "(message)" : contenu);
            msgLabel.setWrapText(true);
            msgLabel.setFont(Font.font("System", 14));
            msgLabel.setTextFill(Color.web("#e2e8f0"));
            bubble.getChildren().add(msgLabel);
        }

        String timeStr = msg.getDateEnvoi() != null
                ? msg.getDateEnvoi().toLocalDateTime().format(timeFmt)
                : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.setTextFill(Color.web("#94a3b8"));
        bubble.getChildren().add(timeLabel);

        ContextMenu ctx = new ContextMenu();
        MenuItem editItem = new MenuItem("Modifier");
        MenuItem deleteItem = new MenuItem("Supprimer");
        editItem.setOnAction(e -> ouvrirEditMessage(msg));
        deleteItem.setOnAction(e -> {
            new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce message ?", ButtonType.OK, ButtonType.CANCEL)
                    .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                messageService.supprimer(msg.getId());
                loadMessages(selectedGroupe.getId());
            });
        });
        ctx.getItems().addAll(editItem, deleteItem);
        bubble.setOnContextMenuRequested(ev -> ctx.show(bubble, ev.getScreenX(), ev.getScreenY()));

        HBox row = new HBox(bubble);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));

        messagesBox.getChildren().add(row);
    }

    private void ajouterLignePieceJointe(VBox bubble, String texte, File f) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.setFont(Font.font("System", 13));
        l.setTextFill(Color.web("#e2e8f0"));
        if (f != null && f.exists()) l.setOnMouseClicked(e -> ouvrirFichier(f));
        bubble.getChildren().add(l);
    }

    private void ouvrirFichier(File f) {
        if (f == null || !f.exists()) {
            new Alert(Alert.AlertType.WARNING, "Fichier introuvable: " + (f != null ? f.getAbsolutePath() : "")).showAndWait();
            return;
        }
        try {
            Desktop.getDesktop().open(f);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le fichier: " + e.getMessage()).showAndWait();
        }
    }

    private void ouvrirCarteNavigateur(double lat, double lon) {
        try {
            String url = "https://www.google.com/maps?q=" + lat + "," + lon;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la carte: " + e.getMessage()).showAndWait();
        }
    }

    private void ouvrirEditMessage(MessageChat msg) {
        TextInputDialog d = new TextInputDialog(msg.getContenu());
        d.setTitle("Modifier le message");
        d.setHeaderText("Nouveau contenu");
        d.showAndWait().ifPresent(nouveauContenu -> {
            if (bannedWordsService.containsBannedWord(nouveauContenu)) {
                new Alert(Alert.AlertType.WARNING, "Message bloqué : langage inapproprié. Veuillez modifier votre message.").showAndWait();
                return;
            }
            msg.setContenu(nouveauContenu);
            messageService.modifier(msg);
            loadMessages(selectedGroupe.getId());
        });
    }

    @FXML
    private void onSend() {
        if (selectedGroupe == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun groupe sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un groupe pour envoyer un message.");
            alert.showAndWait();
            return;
        }

        String text = messageField.getText();
        if (text == null) text = "";
        if (text.trim().isEmpty() && (pendingFichierPath == null || pendingFichierPath.isEmpty())) return;

        if (bannedWordsService.containsBannedWord(text)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Message bloqué");
            alert.setHeaderText("Langage inapproprié");
            alert.setContentText("Votre message contient des mots interdits. Veuillez modifier votre message et respecter les règles de la communauté.");
            alert.showAndWait();
            return;
        }

        try {
            MessageChat newMsg = new MessageChat(text.trim().isEmpty() ? "(pièce jointe)" : text.trim(), selectedGroupe.getId());
            newMsg.setTypeMessage(pendingTypeMessage != null ? pendingTypeMessage : "TEXT");
            newMsg.setFichierPath(pendingFichierPath);
            newMsg.setLatitude(pendingLatitude);
            newMsg.setLongitude(pendingLongitude);
            messageService.ajouter(newMsg);
            pendingFichierPath = null;
            pendingTypeMessage = "TEXT";
            pendingLatitude = null;
            pendingLongitude = null;
            loadMessages(selectedGroupe.getId());
            messageField.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors de l'envoi du message: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onAttach() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Joindre un fichier (image, PDF, audio, vidéo)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("Vidéo", "*.mp4", "*.webm", "*.avi"),
                new FileChooser.ExtensionFilter("Tous", "*.*")
        );
        java.io.File f = fc.showOpenDialog(messageField.getScene().getWindow());
        if (f != null && f.exists()) {
            pendingFichierPath = f.getAbsolutePath();
            String name = f.getName().toLowerCase();
            if (name.endsWith(".pdf")) pendingTypeMessage = "PDF";
            else if (name.matches(".*\\.(png|jpg|jpeg|gif|bmp)")) pendingTypeMessage = "IMAGE";
            else if (name.matches(".*\\.(mp3|wav|m4a)")) pendingTypeMessage = "AUDIO";
            else if (name.matches(".*\\.(mp4|webm|avi)")) pendingTypeMessage = "VIDEO";
            else pendingTypeMessage = "TEXT";
            messageField.setPromptText("Fichier: " + f.getName() + " – tapez un message optionnel");
        }
    }

    @FXML
    private void onLocation() {
        Dialog<String> d = new Dialog<>();
        d.setTitle("📍 Partager ma position actuelle");
        d.setHeaderText("Comme WhatsApp / Messenger : entrez vos coordonnées (ex. Tunis: 36.8, 10.18)");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        TextField latF = new TextField("36.8");
        latF.setPromptText("Latitude");
        TextField lonF = new TextField("10.18");
        lonF.setPromptText("Longitude");
        grid.add(new Label("Latitude:"), 0, 0);
        grid.add(latF, 1, 0);
        grid.add(new Label("Longitude:"), 0, 1);
        grid.add(lonF, 1, 1);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(btn -> btn == ButtonType.OK ? "ok" : null);
        if (d.showAndWait().isEmpty() || d.getResult() == null) return;
        try {
            pendingLatitude = Double.parseDouble(latF.getText().trim());
            pendingLongitude = Double.parseDouble(lonF.getText().trim());
            pendingTypeMessage = "LOCATION";
            messageField.setPromptText("📍 Position actuelle " + String.format("%.2f, %.2f", pendingLatitude, pendingLongitude) + " – message optionnel");
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Latitude et longitude doivent être des nombres (ex. 36.8 et 10.18).").showAndWait();
        }
    }

    @FXML
    private void onNewGroup() {
        Dialog<GroupeChat> dialog = new Dialog<>();
        dialog.setTitle("Nouveau Groupe");
        dialog.setHeaderText("Créer un nouveau groupe de chat");

        ButtonType createButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField nomField = new TextField();
        nomField.setPromptText("Nom du groupe");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("PUBLIC", "PRIVATE");
        typeCombo.setValue("PUBLIC");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String nom = nomField.getText();
                String desc = descField.getText();
                String type = typeCombo.getValue();
                if (nom != null && !nom.trim().isEmpty()) {
                    return new GroupeChat(nom.trim(), desc != null ? desc.trim() : "", type != null ? type : "PUBLIC");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(groupe -> {
            if (groupe != null) {
                groupeService.ajouter(groupe);
                loadGroupes();
            }
        });
    }

    @FXML
    private void onMenu() {
        if (selectedGroupe == null) return;
        
        ContextMenu menu = new ContextMenu();
        MenuItem infoItem = new MenuItem("Informations du groupe");
        MenuItem deleteItem = new MenuItem("Supprimer le groupe");
        
        infoItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informations du groupe");
            alert.setHeaderText(selectedGroupe.getNom());
            alert.setContentText("Description: " + selectedGroupe.getDescription() + "\n" +
                               "Type: " + selectedGroupe.getType() + "\n" +
                               "Date de création: " + selectedGroupe.getDateCreation());
            alert.showAndWait();
        });
        
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le groupe");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedGroupe.getNom() + " ?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    groupeService.supprimer(selectedGroupe.getId());
                    selectedGroupe = null;
                    activeGroupName.setText("Sélectionnez un groupe");
                    messagesBox.getChildren().clear();
                    loadGroupes();
                }
            });
        });
        
        menu.getItems().addAll(infoItem, deleteItem);
        if (btnMenu.getScene() != null && btnMenu.getScene().getWindow() != null) {
            menu.show(btnMenu, 
                btnMenu.getScene().getWindow().getX() + btnMenu.getLayoutX(),
                btnMenu.getScene().getWindow().getY() + btnMenu.getLayoutY() + btnMenu.getHeight());
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (selectedGroupe != null) {
                        loadMessages(selectedGroupe.getId());
                    }
                });
            }
        }, 2000, 2000); // Rafraîchir toutes les 2 secondes
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
