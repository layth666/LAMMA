package controllers;

import entities.GroupeChat;
import entities.MessageChat;
import Services.GroupeChatService;
import Services.MessageChatService;
import javafx.application.Platform;
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

    private GroupeChatService groupeService;
    private MessageChatService messageService;
    private ObservableList<GroupeChat> groupesObservableList;
    private GroupeChat selectedGroupe;
    private Timer refreshTimer;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        groupeService = new GroupeChatService();
        messageService = new MessageChatService();
        groupesObservableList = FXCollections.observableArrayList();

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
        
        // Simuler si c'est l'utilisateur courant (pour la démo, on considère les messages pairs comme "moi")
        boolean isMe = msg.getId() % 2 == 0; // À remplacer par la vraie logique utilisateur
        
        VBox bubble = new VBox(5);
        bubble.getStyleClass().addAll("message-bubble", isMe ? "bubble-me" : "bubble-other");
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 15, 10, 15));

        String contenu = msg.getContenu() != null ? msg.getContenu() : "";
        Label msgLabel = new Label(contenu);
        msgLabel.setWrapText(true);
        msgLabel.setFont(Font.font("System", 14));
        msgLabel.setTextFill(Color.web("#000000"));

        String timeStr = msg.getDateEnvoi() != null 
            ? msg.getDateEnvoi().toLocalDateTime().format(timeFmt) 
            : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.setTextFill(Color.web("#666666"));

        bubble.getChildren().addAll(msgLabel, timeLabel);

        HBox row = new HBox(bubble);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));

        messagesBox.getChildren().add(row);
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
        if (text == null || text.trim().isEmpty()) return;

        try {
            MessageChat newMsg = new MessageChat(text.trim(), selectedGroupe.getId());
            messageService.ajouter(newMsg);
            
            // Recharger les messages pour afficher le nouveau
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
