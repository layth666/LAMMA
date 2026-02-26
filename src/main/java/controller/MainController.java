package controller;

import dao.CommentDAO;
import dao.PostDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import model.Comment;
import model.Post;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private VBox postsContainer;
    @FXML private TextField searchField;
    @FXML private Button btnDefault;
    @FXML private Button btnComments;
    @FXML private Button btnAlpha;
    @FXML private Button btnRecent;
    @FXML private Label bellLabel;

    private String currentSort = "Default";
    private PostDAO postDAO = new PostDAO();
    private CommentDAO commentDAO = new CommentDAO();

    // ── In-memory notifications ──
    private List<String> notifications = new ArrayList<>();

    // ── Cohere API key ──


    // ── Chatbot popup state ──
    private Popup chatPopup;
    private VBox chatMessages;
    private boolean chatOpen = false;

    // ── Bad words list ──
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
            "merde", "putain", "connard", "salaud", "idiot",
            "imbecile", "con", "cul", "fuck", "shit", "bitch",
            "bastard", "asshole", "damn", "crap"
    ));

    // ── Censor helper ──
    private String censorText(String text) {
        if (text == null) return "";
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-ZÀ-ÿ]", "").toLowerCase();
            if (BAD_WORDS.contains(clean)) {
                if (word.length() <= 2) {
                    result.append(word);
                } else {
                    result.append(word.charAt(0));
                    result.append("*".repeat(word.length() - 2));
                    result.append(word.charAt(word.length() - 1));
                }
            } else {
                result.append(word);
            }
            result.append(" ");
        }
        return result.toString().trim();
    }

    // ── Bell update ──
    private void updateBell() {
        int count = notifications.size();
        if (count > 0) {
            bellLabel.setText("🔔 " + count);
            bellLabel.getStyleClass().setAll("bell-active");
        } else {
            bellLabel.setText("🔔");
            bellLabel.getStyleClass().setAll("bell-label");
        }
    }

    // ── Bell click handler ──
    @FXML
    private void handleBellClick() {
        if (notifications.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notifications");
            alert.setHeaderText(null);
            alert.setContentText("Aucune nouvelle notification.");
            alert.showAndWait();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String n : notifications) {
            sb.append("• ").append(n).append("\n");
        }
        notifications.clear();
        updateBell();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Vos notifications");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // ── Cohere chat API ──
    private String askCohere(String userMessage) {
        try {
            String systemPrompt = "You are a camping assistant. You ONLY answer questions about camping, "
                    + "hiking, outdoor survival, tents, sleeping bags, campfires, nature, and related outdoor topics. "
                    + "If the user asks about anything unrelated to camping or outdoors, politely refuse and say: "
                    + "Je suis uniquement disponible pour repondre aux questions sur le camping et les activites en plein air. "
                    + "Always reply in the same language the user wrote in (French or English). Keep answers short and helpful.";

            String json = "{"
                    + "\"model\":\"command-r\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(userMessage) + "}],"
                    + "\"system\":" + escapeJson(systemPrompt) + ","
                    + "\"max_tokens\":300"
                    + "}";

            URL url = new URL("https://api.cohere.com/v2/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + COHERE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader reader;
            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String resp = response.toString();
            System.out.println("COHERE RAW: " + resp); // debug

            // Try multiple parse strategies
            String result = null;

            // Strategy 1: "text":"..."  (v2 content array)
            String key1 = "\"text\":\"";
            int s1 = resp.indexOf(key1);
            if (s1 >= 0) {
                s1 += key1.length();
                // find closing quote not preceded by backslash
                int e1 = s1;
                while (e1 < resp.length()) {
                    if (resp.charAt(e1) == '"' && resp.charAt(e1 - 1) != '\\') break;
                    e1++;
                }
                if (e1 < resp.length()) result = resp.substring(s1, e1);
            }

            // Strategy 2: "generations":[{"text":"..."}]  (generate endpoint fallback)
            if (result == null || result.isEmpty()) {
                String key2 = "\"text\":\"";
                int s2 = resp.lastIndexOf(key2);
                if (s2 >= 0) {
                    s2 += key2.length();
                    int e2 = resp.indexOf("\"", s2);
                    if (e2 >= 0) result = resp.substring(s2, e2);
                }
            }

            if (result == null || result.isEmpty()) return "Pas de réponse reçue.";
            return result.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur de connexion. Vérifie ta connexion internet.";
        }
    }

    private String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // ── Build chatbot popup ──
    private void buildChatPopup() {
        chatPopup = new Popup();
        chatPopup.setAutoHide(false);

        VBox container = new VBox(0);
        container.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        container.setPrefWidth(320);
        container.setMaxHeight(480);

        // Header
        HBox header = new HBox(10);
        header.setStyle(
                "-fx-background-color: #1a3a5c;" +
                        "-fx-padding: 12 16;" +
                        "-fx-background-radius: 12 12 0 0;"
        );
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label headerIcon = new Label("⛺");
        headerIcon.setStyle("-fx-font-size: 18px;");

        VBox headerText = new VBox(2);
        Label headerTitle = new Label("Camping Assistant");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        Label headerSub = new Label("Ask me anything about camping!");
        headerSub.setStyle("-fx-text-fill: #cdd8e3; -fx-font-size: 11px;");
        headerText.getChildren().addAll(headerTitle, headerSub);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-color: transparent;"
        );
        closeBtn.setOnAction(e -> {
            chatPopup.hide();
            chatOpen = false;
        });

        header.getChildren().addAll(headerIcon, headerText, closeBtn);

        // Messages area
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: transparent;");

        chatMessages = new VBox(8);
        chatMessages.setStyle("-fx-padding: 12;");
        scrollPane.setContent(chatMessages);

        // Welcome message
        addBotMessage("Bonjour! 🏕️ Je suis ton assistant camping. Pose-moi des questions sur le camping, la randonnée, le matériel ou la survie en plein air!");

        // Input area
        HBox inputArea = new HBox(8);
        inputArea.setStyle(
                "-fx-padding: 10 12;" +
                        "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1 0 0 0;"
        );
        inputArea.setAlignment(javafx.geometry.Pos.CENTER);

        TextField chatInput = new TextField();
        chatInput.setPromptText("Pose une question sur le camping...");
        chatInput.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-border-color: transparent;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 13px;"
        );
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color: #1a3a5c;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 14;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );

        // Send action
        Runnable sendMessage = () -> {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty()) return;
            chatInput.clear();
            addUserMessage(msg);

            // Typing indicator
            Label typing = new Label("⛺ typing...");
            typing.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 4 8;");
            chatMessages.getChildren().add(typing);
            scrollToBottom(scrollPane);

            Thread t = new Thread(() -> {
                String response = askCohere(msg);
                Platform.runLater(() -> {
                    chatMessages.getChildren().remove(typing);
                    addBotMessage(response);
                    scrollToBottom(scrollPane);
                });
            });
            t.setDaemon(true);
            t.start();
        };

        sendBtn.setOnAction(e -> sendMessage.run());
        chatInput.setOnAction(e -> sendMessage.run());

        inputArea.getChildren().addAll(chatInput, sendBtn);
        container.getChildren().addAll(header, scrollPane, inputArea);
        chatPopup.getContent().add(container);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(220);
        msg.setStyle(
                "-fx-background-color: #1a3a5c;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 12 12 2 12;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 13px;"
        );
        row.getChildren().add(msg);
        chatMessages.getChildren().add(row);
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(6);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("⛺");
        icon.setStyle("-fx-font-size: 14px;");
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(220);
        msg.setStyle(
                "-fx-background-color: #e8f0fe;" +
                        "-fx-text-fill: #1a3a5c;" +
                        "-fx-background-radius: 12 12 12 2;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 13px;"
        );
        row.getChildren().addAll(icon, msg);
        chatMessages.getChildren().add(row);
    }

    private void scrollToBottom(ScrollPane sp) {
        Platform.runLater(() -> sp.setVvalue(1.0));
    }

    // ── Unsplash image fetch ──
    private String fetchImageUrl(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String urlStr = "https://api.unsplash.com/photos/random?query=" + encoded + "&client_id=" + UNSPLASH_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            String key = "\"small\":\"";
            int start = json.indexOf(key) + key.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Translation helper ──
    private String translateText(String text, String fromLang, String toLang) {
        try {
            String encoded = URLEncoder.encode(text, "UTF-8");
            String urlStr = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=" + fromLang + "|" + toLang;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            String key = "\"translatedText\":\"";
            int start = json.indexOf(key) + key.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return "Translation failed.";
        }
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadPosts());
        updateBell();
        buildChatPopup();
        loadPosts();
    }

    // ── Chat button handler ──
    @FXML
    private void handleChatBtn() {
        if (chatOpen) {
            chatPopup.hide();
            chatOpen = false;
        } else {
            javafx.stage.Window window = postsContainer.getScene().getWindow();
            double x = window.getX() + window.getWidth() - 345;
            double y = window.getY() + window.getHeight() - 530;
            chatPopup.show(window, x, y);
            chatOpen = true;
        }
    }

    // ── Sort handlers ──
    @FXML private void handleSortDefault() { currentSort = "Default"; setActiveFilter(btnDefault); loadPosts(); }
    @FXML private void handleSortComments() { currentSort = "Most Comments"; setActiveFilter(btnComments); loadPosts(); }
    @FXML private void handleSortAlpha() { currentSort = "Alphabetical"; setActiveFilter(btnAlpha); loadPosts(); }
    @FXML private void handleSortRecent() { currentSort = "Recent"; setActiveFilter(btnRecent); loadPosts(); }

    private void setActiveFilter(Button active) {
        btnDefault.getStyleClass().setAll("filter-btn");
        btnComments.getStyleClass().setAll("filter-btn");
        btnAlpha.getStyleClass().setAll("filter-btn");
        btnRecent.getStyleClass().setAll("filter-btn");
        active.getStyleClass().setAll("filter-btn-active");
    }

    @FXML
    private void handleAddPost() {
        String title = titleField.getText();
        String content = contentArea.getText();
        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Erreur", "Le titre et le contenu ne peuvent pas être vides.");
            return;
        }
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        postDAO.createPost(post);
        titleField.clear();
        contentArea.clear();
        loadPosts();
    }

    @FXML
    private void handleRefresh() { loadPosts(); }

    private void loadPosts() {
        postsContainer.getChildren().clear();
        List<Post> posts = postDAO.getAllPosts();

        String search = searchField.getText().toLowerCase().trim();
        if (!search.isEmpty()) {
            posts = posts.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }

        if ("Alphabetical".equals(currentSort)) {
            posts.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        } else if ("Most Comments".equals(currentSort)) {
            posts.sort((a, b) -> {
                int countA = commentDAO.getCommentsByPostId(a.getId()).size();
                int countB = commentDAO.getCommentsByPostId(b.getId()).size();
                return Integer.compare(countB, countA);
            });
        } else if ("Recent".equals(currentSort)) {
            posts.sort((a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }

        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "";
        long minutes = java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "à l'instant";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " h ago";
        long days = hours / 24;
        if (days < 7) return days + " j ago";
        long weeks = days / 7;
        return weeks + " sem ago";
    }

    private VBox createPostCard(Post post) {
        VBox postBox = new VBox(12);
        postBox.getStyleClass().add("post-card");

        // ── Unsplash image ──
        ImageView imageView = new ImageView();
        imageView.setFitWidth(580);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("post-image");
        imageView.setVisible(false);
        imageView.setManaged(false);

        Thread imgThread = new Thread(() -> {
            String imageUrl = fetchImageUrl(post.getTitle());
            if (imageUrl != null) {
                Image image = new Image(imageUrl, true);
                Platform.runLater(() -> {
                    imageView.setImage(image);
                    imageView.setVisible(true);
                    imageView.setManaged(true);
                });
            }
        });
        imgThread.setDaemon(true);
        imgThread.start();

        // ── Top bar ──
        HBox topBar = new HBox();
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label metaLabel = new Label("Posté par u/user  •  " + timeAgo(post.getCreatedAt()));
        metaLabel.getStyleClass().add("post-meta");
        HBox.setHgrow(metaLabel, Priority.ALWAYS);

        HBox cardActions = new HBox(6);
        cardActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button editPostBtn = new Button("✏");
        editPostBtn.getStyleClass().add("edit-button");
        editPostBtn.setOnAction(e -> {
            TextInputDialog titleDialog = new TextInputDialog(post.getTitle());
            titleDialog.setTitle("Modifier Post");
            titleDialog.setHeaderText("Modifier le titre");
            titleDialog.setContentText("Nouveau titre:");
            titleDialog.showAndWait().ifPresent(newTitle -> {
                TextInputDialog contentDialog = new TextInputDialog(post.getContent());
                contentDialog.setTitle("Modifier Post");
                contentDialog.setHeaderText("Modifier le contenu");
                contentDialog.setContentText("Nouveau contenu:");
                contentDialog.showAndWait().ifPresent(newContent -> {
                    postDAO.updatePost(post.getId(), newTitle, newContent);
                    loadPosts();
                });
            });
        });

        Button deletePostBtn = new Button("🗑");
        deletePostBtn.getStyleClass().add("delete-button");
        deletePostBtn.setOnAction(e -> {
            postDAO.deletePost(post.getId());
            loadPosts();
        });

        cardActions.getChildren().addAll(editPostBtn, deletePostBtn);
        topBar.getChildren().addAll(metaLabel, cardActions);

        Label titleLabel = new Label(censorText(post.getTitle()));
        titleLabel.getStyleClass().add("post-title");

        Label contentLabel = new Label(censorText(post.getContent()));
        contentLabel.getStyleClass().add("post-content");
        contentLabel.setWrapText(true);

        // ── Translate button ──
        HBox translateBar = new HBox(8);
        translateBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button translateBtn = new Button("🌐 Traduire en anglais");
        translateBtn.getStyleClass().add("translate-button");
        translateBtn.setOnAction(e -> {
            translateBtn.setText("⏳ Traduction...");
            translateBtn.setDisable(true);
            Thread thread = new Thread(() -> {
                String translatedTitle = translateText(post.getTitle(), "fr", "en");
                String translatedContent = translateText(post.getContent(), "fr", "en");
                Platform.runLater(() -> {
                    titleLabel.setText(translatedTitle);
                    contentLabel.setText(translatedContent);
                    translateBtn.setText("🔄 Voir original");
                    translateBtn.setDisable(false);
                    translateBtn.setOnAction(ev -> {
                        titleLabel.setText(censorText(post.getTitle()));
                        contentLabel.setText(censorText(post.getContent()));
                        translateBtn.setText("🌐 Traduire en anglais");
                        translateBtn.setOnAction(orig -> {
                            translateBtn.setText("⏳ Traduction...");
                            translateBtn.setDisable(true);
                            Thread t2 = new Thread(() -> {
                                String t1 = translateText(post.getTitle(), "fr", "en");
                                String t2c = translateText(post.getContent(), "fr", "en");
                                Platform.runLater(() -> {
                                    titleLabel.setText(t1);
                                    contentLabel.setText(t2c);
                                    translateBtn.setText("🔄 Voir original");
                                    translateBtn.setDisable(false);
                                });
                            });
                            t2.setDaemon(true);
                            t2.start();
                        });
                    });
                });
            });
            thread.setDaemon(true);
            thread.start();
        });

        translateBar.getChildren().add(translateBtn);

        List<Comment> comments = commentDAO.getCommentsByPostId(post.getId());
        Label commentCount = new Label("💬  " + comments.size() + " commentaires");
        commentCount.getStyleClass().add("post-meta");

        Separator sep = new Separator();

        VBox commentsBox = new VBox(8);
        commentsBox.getStyleClass().add("comments-section");

        for (Comment comment : comments) {
            HBox commentRow = new HBox(8);
            commentRow.getStyleClass().add("comment-row");
            commentRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label commentLabel = new Label("💬  " + censorText(comment.getContent()));
            commentLabel.getStyleClass().add("comment-text");
            HBox.setHgrow(commentLabel, Priority.ALWAYS);
            commentLabel.setWrapText(true);

            HBox commentActions = new HBox(4);
            commentActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button editCommentBtn = new Button("✏");
            editCommentBtn.getStyleClass().add("edit-button");
            editCommentBtn.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog(comment.getContent());
                dialog.setTitle("Modifier Commentaire");
                dialog.setHeaderText("Modifier votre commentaire");
                dialog.setContentText("Nouveau commentaire:");
                dialog.showAndWait().ifPresent(newContent -> {
                    commentDAO.updateComment(comment.getId(), newContent);
                    loadPosts();
                });
            });

            Button deleteCommentBtn = new Button("✕");
            deleteCommentBtn.getStyleClass().add("delete-button");
            deleteCommentBtn.setOnAction(ev -> {
                commentDAO.deleteComment(comment.getId());
                loadPosts();
            });

            commentActions.getChildren().addAll(editCommentBtn, deleteCommentBtn);
            commentRow.getChildren().addAll(commentLabel, commentActions);
            commentsBox.getChildren().add(commentRow);
        }

        HBox addCommentBox = new HBox(8);
        addCommentBox.getStyleClass().add("add-comment-box");
        addCommentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField commentField = new TextField();
        commentField.setPromptText("Écrire un commentaire...");
        commentField.getStyleClass().add("comment-field");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button addCommentBtn = new Button("Commenter");
        addCommentBtn.getStyleClass().add("comment-button");
        addCommentBtn.setOnAction(e -> {
            if (commentField.getText().isEmpty()) {
                showAlert("Erreur", "Le commentaire ne peut pas être vide.");
                return;
            }
            Comment comment = new Comment();
            comment.setContent(commentField.getText());
            comment.setPostId(post.getId());
            commentDAO.createComment(comment);

            // ── Add notification ──
            notifications.add("Nouveau commentaire sur : \"" + post.getTitle() + "\"");
            updateBell();

            loadPosts();
        });

        addCommentBox.getChildren().addAll(commentField, addCommentBtn);

        postBox.getChildren().addAll(imageView, topBar, titleLabel, contentLabel, translateBar, commentCount, sep, commentsBox, addCommentBox);
        return postBox;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}