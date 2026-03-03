package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Popup;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.User;
import service.UserService;
import service.GoogleAuthService;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {


    @FXML
    private VBox detailsSidebar;

    private boolean sidebarVisible = true;

    private final double SIDEBAR_WIDTH = 350;


    @FXML
    private Label profilePhone;

    @FXML
    private Label profileMotorized;

    @FXML
    private Button deleteUserBtn;


    @FXML
    private TextField searchField;

    @FXML
    private Label profileName;

    @FXML
    private Label profileEmail;

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label adminUsersLabel;

    @FXML
    private Label regularUsersLabel;

    @FXML
    private Label profileRole;

    @FXML
    private GridPane usersGrid;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterAlphabetBtn;

    @FXML
    private Button filterLatestBtn;

    @FXML
    private Label topBarUserName;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Circle topBarUserAvatar;

    @FXML
    private Circle profileAvatar;

    private UserService userService = new UserService();
    private User currentUser;
    private User selectedUser;

    @FXML
    public void initialize() {
        loadUsers();
        updateStatistics();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                List<User> users = userService.recuperer();
                List<User> filtered = users.stream()
                        .filter(u ->
                                u.getName().toLowerCase().contains(newVal.toLowerCase())
                                        ||
                                        u.getEmail().toLowerCase().contains(newVal.toLowerCase())
                        )
                        .toList();
                displayUsers(filtered);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateStatistics() {
        try {
            List<User> users = userService.recuperer();

            if (totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(users.size()));
            }

            long adminCount = users.stream()
                    .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                    .count();
            if (adminUsersLabel != null) {
                adminUsersLabel.setText(String.valueOf(adminCount));
            }

            long regularCount = users.stream()
                    .filter(u -> "USER".equalsIgnoreCase(u.getRole()))
                    .count();
            if (regularUsersLabel != null) {
                regularUsersLabel.setText(String.valueOf(regularCount));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Dashboard loaded for admin: " + user.getName());

        if (profileName != null) {
            profileName.setText(user.getName());
            profileEmail.setText(user.getEmail());
            profileRole.setText(user.getRole());
        }

        if (topBarUserName != null) {
            topBarUserName.setText(user.getName());
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + user.getName());
        }

        // Load avatars
        if (topBarUserAvatar != null) {
            loadUserAvatar(topBarUserAvatar, user, 20);
        }

        if (profileAvatar != null) {
            loadUserAvatar(profileAvatar, user, 65);
        }
    }

    /**
     * ✅ FIXED: Load avatar with proper validation and error handling
     */
    private void loadUserAvatar(Circle avatarCircle, User user, double radius) {
        String imageUrl = user.getImage();

        // ✅ FIX: Comprehensive URL validation
        if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                !imageUrl.equalsIgnoreCase("null") &&
                (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {

            System.out.println("📥 Loading image for: " + user.getName() + " from " + imageUrl);

            try {
                // Load image in background
                Image image = new Image(imageUrl, true);

                // Set default avatar first
                setDefaultAvatar(avatarCircle, user, radius);

                // Wait for image to load before applying
                image.progressProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        if (newValue.doubleValue() >= 1.0) {
                            // Image fully loaded
                            Platform.runLater(() -> {
                                if (!image.isError()) {
                                    try {
                                        avatarCircle.setFill(new ImagePattern(image));
                                        System.out.println("✅ Image loaded successfully for: " + user.getName());
                                    } catch (Exception e) {
                                        System.err.println("❌ Error applying image for " + user.getName() + ": " + e.getMessage());
                                        setDefaultAvatar(avatarCircle, user, radius);
                                    }
                                } else {
                                    System.err.println("❌ Image load error for: " + user.getName());
                                    setDefaultAvatar(avatarCircle, user, radius);
                                }
                            });
                        }
                    }
                });

                // Also listen for errors
                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        System.err.println("❌ Image error for: " + user.getName() + " - URL: " + imageUrl);
                        Platform.runLater(() -> setDefaultAvatar(avatarCircle, user, radius));
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Invalid image URL for " + user.getName() + ": " + imageUrl);
                System.err.println("   Error: " + e.getMessage());
                setDefaultAvatar(avatarCircle, user, radius);
            }

        } else {
            System.out.println("ℹ️ No valid image URL for: " + user.getName() + " (URL: " + imageUrl + ")");
            setDefaultAvatar(avatarCircle, user, radius);
        }
    }

    private void setDefaultAvatar(Circle avatarCircle, User user, double radius) {
        avatarCircle.setFill(Color.web(getUserColor(user)));
        avatarCircle.setRadius(radius);
    }

    private String getUserColor(User user) {
        String[] colors = {
                "#7B5FF5", "#10b981", "#ef4444", "#f59e0b",
                "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6"
        };
        int index = Math.abs(user.getName().hashCode()) % colors.length;
        return colors[index];
    }

    private void loadUsers() {
        try {
            List<User> users = userService.recuperer();
            displayUsers(users);
        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Error loading users: " + e.getMessage(), "error");
        }
    }

    private void displayUsers(List<User> users) {
        usersGrid.getChildren().clear();

        int column = 0;
        int row = 0;
        int maxColumns = 3;

        for (User user : users) {
            VBox userCard = createUserCard(user);
            usersGrid.add(userCard, column, row);

            column++;
            if (column == maxColumns) {
                column = 0;
                row++;
            }
        }
    }

    /**
     * ✅ FIXED: Create user card with proper image loading
     */
    private VBox createUserCard(User user) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(14);
        card.setPadding(new Insets(18));

        card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.4);" +
                        "-fx-border-color: rgba(100, 116, 139, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);"
        );
        card.setPrefWidth(280);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.6);" +
                        "-fx-border-color: rgba(255, 140, 0, 0.6);" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255, 99, 71, 0.4), 20, 0, 0, 5);"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.4);" +
                        "-fx-border-color: rgba(100, 116, 139, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);"
        ));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(14);

        StackPane avatarContainer = new StackPane();
        Circle avatar = new Circle(25);

        Label initial = new Label(user.getName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-size: 18; -fx-font-weight: 700; -fx-text-fill: white;");

        String imageUrl = user.getImage();

        if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                !imageUrl.equalsIgnoreCase("null") &&
                (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {

            try {
                Image image = new Image(imageUrl, true);
                avatar.setFill(Color.web(getUserColor(user)));

                image.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() >= 1.0) {
                        Platform.runLater(() -> {
                            if (!image.isError()) {
                                try {
                                    avatar.setFill(new ImagePattern(image));
                                    initial.setVisible(false);
                                } catch (Exception e) {
                                    System.err.println("❌ Image error: " + user.getName());
                                }
                            }
                        });
                    }
                });

                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        System.err.println("❌ Image load error: " + user.getName());
                    }
                });

            } catch (Exception e) {
                avatar.setFill(Color.web(getUserColor(user)));
            }
        } else {
            avatar.setFill(Color.web(getUserColor(user)));
        }

        avatarContainer.getChildren().addAll(avatar, initial);

        VBox userInfo = new VBox();
        userInfo.setSpacing(3);

        Label userName = new Label(user.getName());
        userName.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #e2e8f0;");

        Label userRole = new Label(user.getRole());
        userRole.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-font-weight: 500;");

        userInfo.getChildren().addAll(userName, userRole);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button menuBtn = new Button("⋮");
        menuBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        );

        menuBtn.setOnAction(e -> showUserMenu(e, user, card));

        header.getChildren().addAll(avatarContainer, userInfo, spacer, menuBtn);

        Label email = new Label(user.getEmail());
        email.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
        email.setWrapText(true);

        card.getChildren().addAll(header, email);
        card.setOnMouseClicked(e -> showUserDetails(user));

        return card;
    }

    private void showUserDetails(User user) {
        this.selectedUser = user;

        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profileRole.setText(user.getRole());

        // NEW
        profilePhone.setText(
                user.getPhone() != null && !user.getPhone().isEmpty()
                        ? user.getPhone()
                        : "-"
        );

        String motorized = user.getMotorized();
        if ("YES".equalsIgnoreCase(motorized)) {
            profileMotorized.setText("Yes");
            profileMotorized.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 600;");
        } else {
            profileMotorized.setText("No");
            profileMotorized.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
        }

        loadUserAvatar(profileAvatar, user, 65);

        if (!sidebarVisible) {
            showSidebar();
        }
    }

    private void showUserMenu(ActionEvent e, User user, VBox card) {
        VBox popupContent = new VBox(8);
        popupContent.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 6;" +
                        "-fx-min-width: 160;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 6);"
        );

        String[] actions = {"⭐ Add to favourites", "⬆ Promote", "🚫 Ban", "🗑 Delete"};
        for (String actionText : actions) {
            Label action = new Label(actionText);
            styleMenuItem(action, actionText.contains("Delete"));

            action.setOnMouseClicked(ev -> {
                Popup popup = (Popup) action.getScene().getWindow();
                try {
                    if (actionText.contains("Delete")) {
                        userService.supprimer(user.getId());
                        usersGrid.getChildren().remove(card);
                        updateStatistics();
                        showToast("User deleted successfully!", "success");
                    } else if (actionText.contains("Promote")) {
                        if (!user.getRole().equalsIgnoreCase("ADMIN")) {
                            user.setRole("ADMIN");
                            userService.modifier(user);

                            HBox headerBox = (HBox) card.getChildren().get(0);
                            VBox userInfoBox = (VBox) headerBox.getChildren().get(1);
                            Label roleLabel = (Label) userInfoBox.getChildren().get(1);
                            roleLabel.setText("ADMIN");

                            updateStatistics();
                            showToast(user.getName() + " promoted to ADMIN!", "success");
                        } else {
                            showToast(user.getName() + " is already an ADMIN!", "info");
                        }
                    } else if (actionText.contains("Ban")) {
                        showToast(user.getName() + " has been banned!", "info");
                    } else if (actionText.contains("favourites")) {
                        showToast(user.getName() + " added to favourites!", "success");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Error: " + ex.getMessage(), "error");
                }
                popup.hide();
            });

            popupContent.getChildren().add(action);
        }

        Popup popup = new Popup();
        popup.getContent().add(popupContent);
        popup.setAutoHide(true);

        Node btnNode = (Node) e.getSource();
        double x = btnNode.localToScreen(btnNode.getBoundsInLocal()).getMinX() - 100 + btnNode.getBoundsInLocal().getWidth();
        double y = btnNode.localToScreen(btnNode.getBoundsInLocal()).getMinY() + btnNode.getBoundsInLocal().getHeight() + 4;
        popup.show(btnNode, x, y);

        popupContent.setOpacity(0);
        popupContent.setScaleX(0.8);
        popupContent.setScaleY(0.8);

        FadeTransition fade = new FadeTransition(Duration.millis(200), popupContent);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popupContent);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1);
        scale.setToY(1);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.play();
    }

    @FXML
    private void showDashboard() {
        // Dashboard is already showing
    }

    private void showToast(String message, String type) {
        System.out.println(type.toUpperCase() + ": " + message);
    }

    @FXML
    private void handleAddUser() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Member");

        // MAIN CONTAINER (DARK GLASS)
        VBox mainContainer = new VBox();
        mainContainer.setPrefWidth(650);
        mainContainer.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #2b3445, #1f2937);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 25, 0, 0, 8);"
        );

        // ================= HEADER =================
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(28));
        header.setSpacing(15);
        header.setStyle(
                "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        VBox headerText = new VBox(5);

        Label title = new Label("New Member");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: 700; -fx-text-fill: #f1f5f9;");

        Label subtitle = new Label("Create a new user account");
        subtitle.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");

        headerText.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 16;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> dialogStage.close());

        header.getChildren().addAll(headerText, spacer, closeBtn);

        // ================= FORM CONTENT =================
        VBox formContent = new VBox(22);
        formContent.setPadding(new Insets(30));

        String labelStyle = "-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #cbd5e1;";
        String fieldStyle =
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 12;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-prompt-text-fill: #64748b;" +
                        "-fx-font-size: 14;";

        // NAME
        VBox nameGroup = new VBox(8);
        Label nameLabel = new Label("FULL NAME");
        nameLabel.setStyle(labelStyle);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter full name");
        nameField.setStyle(fieldStyle);
        nameField.setPrefHeight(45);

        nameGroup.getChildren().addAll(nameLabel, nameField);

        // EMAIL
        VBox emailGroup = new VBox(8);
        Label emailLabel = new Label("EMAIL ADDRESS");
        emailLabel.setStyle(labelStyle);

        TextField emailField = new TextField();
        emailField.setPromptText("user@example.com");
        emailField.setStyle(fieldStyle);
        emailField.setPrefHeight(45);

        emailGroup.getChildren().addAll(emailLabel, emailField);

        // PASSWORD
        VBox passwordGroup = new VBox(8);
        Label passwordLabel = new Label("PASSWORD");
        passwordLabel.setStyle(labelStyle);

        StackPane passwordContainer = new StackPane();

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 characters");
        passwordField.setStyle(fieldStyle);
        passwordField.setPrefHeight(45);

        TextField passwordVisible = new TextField();
        passwordVisible.setPromptText("Minimum 6 characters");
        passwordVisible.setStyle(fieldStyle);
        passwordVisible.setPrefHeight(45);
        passwordVisible.setVisible(false);

        passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());

        Button eyeBtn = new Button("👁");
        eyeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-cursor: hand;"
        );
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeBtn, new Insets(0, 12, 0, 0));

        eyeBtn.setOnAction(e -> {
            if (passwordField.isVisible()) {
                passwordField.setVisible(false);
                passwordVisible.setVisible(true);
                eyeBtn.setText("🙈");
            } else {
                passwordField.setVisible(true);
                passwordVisible.setVisible(false);
                eyeBtn.setText("👁");
            }
        });

        passwordContainer.getChildren().addAll(passwordField, passwordVisible, eyeBtn);

        Label passwordHint = new Label("Must contain uppercase, lowercase, special character");
        passwordHint.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");

        passwordGroup.getChildren().addAll(passwordLabel, passwordContainer, passwordHint);

        // PHONE
        VBox phoneGroup = new VBox(8);
        Label phoneLabel = new Label("PHONE NUMBER");
        phoneLabel.setStyle(labelStyle);

        TextField phoneInput = new TextField();
        phoneInput.setPromptText("Enter phone number");
        phoneInput.setStyle(fieldStyle);
        phoneInput.setPrefHeight(45);

        phoneGroup.getChildren().addAll(phoneLabel, phoneInput);

        // ROLE
        VBox roleGroup = new VBox(8);
        Label roleLabel = new Label("ROLE");
        roleLabel.setStyle(labelStyle);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("USER", "ADMIN");
        roleBox.setValue("USER");
        roleBox.setStyle(fieldStyle);
        roleBox.setPrefHeight(45);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        roleGroup.getChildren().addAll(roleLabel, roleBox);

        formContent.getChildren().addAll(
                nameGroup,
                emailGroup,
                passwordGroup,
                phoneGroup,
                roleGroup
        );

        // ================= FOOTER =================
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(25));
        footer.setStyle(
                "-fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width: 1 0 0 0;"
        );

        Button cancelBtn = new Button("Abort Mission");
        cancelBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 12 24;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button saveBtn = new Button("🚀 Create User");
        saveBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #FF8C00, #FF6347);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 600;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 12 26;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,99,71,0.4), 10,0,0,2);"
        );

        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showDialogToast(formContent, "Please enter a name", "error");
                return;
            }

            if (emailField.getText().trim().isEmpty()) {
                showDialogToast(formContent, "Please enter an email", "error");
                return;
            }

            if (passwordField.getText().trim().isEmpty() || passwordField.getText().length() < 6) {
                showDialogToast(formContent, "Password must be at least 6 characters", "error");
                return;
            }

            try {
                String hashedPassword = utils.PasswordHasher.hashPassword(passwordField.getText());
                User newUser = new User(
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        hashedPassword,
                        roleBox.getValue(),
                        phoneInput.getText(),
                        "NO",
                        null
                );

                userService.ajouter(newUser);
                loadUsers();
                updateStatistics();
                showToast("User created successfully! 🎉", "success");
                dialogStage.close();

            } catch (SQLException ex) {
                showDialogToast(formContent, "Error: " + ex.getMessage(), "error");
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);

        mainContainer.getChildren().addAll(header, formContent, footer);

        Scene dialogScene = new Scene(mainContainer);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.show();
    }

    private void showDialogToast(VBox container, String message, String type) {
        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER);
        toast.setPadding(new Insets(12, 16, 12, 16));
        toast.setStyle(
                "-fx-background-color: " + (type.equals("error") ? "#fee2e2" : "#d1fae5") + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + (type.equals("error") ? "#ef4444" : "#10b981") + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 8;"
        );

        Label icon = new Label(type.equals("error") ? "⚠️" : "✅");
        icon.setStyle("-fx-font-size: 16;");

        Label msg = new Label(message);
        msg.setStyle(
                "-fx-font-size: 13;" +
                        "-fx-text-fill: " + (type.equals("error") ? "#dc2626" : "#047857") + ";" +
                        "-fx-font-weight: 600;"
        );

        toast.getChildren().addAll(icon, msg);

        container.getChildren().add(0, toast);

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> container.getChildren().remove(toast));
        pause.play();
    }

    @FXML
    private void sortAlphabet() {
        try {
            animateFilterButton(filterAlphabetBtn);
            List<User> users = userService.recuperer();
            users.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            displayUsers(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void sortRecent() {
        animateFilterButton(filterAllBtn);
        loadUsers();
    }

    @FXML
    private void sortLatest() {
        try {
            animateFilterButton(filterLatestBtn);
            List<User> users = userService.recuperer();
            users.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
            displayUsers(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void animateFilterButton(Button selectedButton) {
        filterAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-background-radius: 8; -fx-font-size: 13; -fx-font-weight: 500; -fx-padding: 10 20; -fx-cursor: hand;");
        filterAlphabetBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-background-radius: 8; -fx-font-size: 13; -fx-font-weight: 500; -fx-padding: 10 20; -fx-cursor: hand;");
        filterLatestBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-background-radius: 8; -fx-font-size: 13; -fx-font-weight: 500; -fx-padding: 10 20; -fx-cursor: hand;");

        selectedButton.setScaleX(0.95);
        selectedButton.setScaleY(0.95);

        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), selectedButton);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), selectedButton);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);

        scaleIn.setOnFinished(e -> {
            selectedButton.setStyle("-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 13; -fx-font-weight: 600; -fx-padding: 10 20; -fx-cursor: hand;");
        });

        scaleIn.play();
        fade.play();
    }

    @FXML
    private void closeWindow() {
        dashboardBtn.getScene().getWindow().hide();
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) dashboardBtn.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) dashboardBtn.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            GoogleAuthService.logout();
            utils.Session.getInstance().logout();

            Parent root = FXMLLoader.load(getClass().getResource("/User.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleMenuItem(Label action, boolean isDelete) {
        String normalColor = isDelete ? "#e74c3c" : "#212529";
        String hoverColor = isDelete ? "#c0392b" : "#7B5FF5";

        action.setStyle(
                "-fx-font-size: 13;" +
                        "-fx-text-fill: " + normalColor + ";" +
                        "-fx-padding: 8 14;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: 500;"
        );

        action.setOnMouseEntered(e ->
                action.setStyle(
                        "-fx-font-size: 13;" +
                                "-fx-text-fill: " + hoverColor + ";" +
                                "-fx-padding: 8 14;" +
                                "-fx-background-radius: 8;" +
                                "-fx-background-color: #f1f5f9;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-weight: 500;"
                )
        );

        action.setOnMouseExited(e ->
                action.setStyle(
                        "-fx-font-size: 13;" +
                                "-fx-text-fill: " + normalColor + ";" +
                                "-fx-padding: 8 14;" +
                                "-fx-background-radius: 8;" +
                                "-fx-background-color: transparent;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-weight: 500;"
                )
        );
    }

    @FXML
    private void handleDeleteFromDetails() {
        if (selectedUser == null) {
            showToast("No user selected", "error");
            return;
        }

        try {
            userService.supprimer(selectedUser.getId());

            showToast("User deleted successfully!", "success");

            loadUsers();
            updateStatistics();

            // Clear sidebar
            profileName.setText("Select a user");
            profileEmail.setText("-");
            profilePhone.setText("-");
            profileMotorized.setText("-");
            profileRole.setText("ROLE");

            selectedUser = null;

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Error deleting user", "error");
        }
    }

    @FXML
    private void toggleSidebar() {

        if (sidebarVisible) {
            hideSidebar();
        } else {
            showSidebar();
        }
    }

    private void hideSidebar() {

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(detailsSidebar.prefWidthProperty(), 0)
                )
        );

        timeline.play();
        sidebarVisible = false;
    }

    private void showSidebar() {

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(detailsSidebar.prefWidthProperty(), SIDEBAR_WIDTH)
                )
        );

        timeline.play();
        sidebarVisible = true;
    }
}