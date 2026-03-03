package controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.GoogleAuthService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.function.Consumer;

/**
 * Professional Google Authentication Dialog with WebView
 * Displays embedded browser for Google sign-in
 */
public class GoogleAuthDialog {

    private Stage dialogStage;
    private WebView webView;
    private WebEngine webEngine;
    private Consumer<GoogleAuthService.GoogleUserInfo> onSuccess;
    private Consumer<Exception> onError;
    private ProgressBar progressBar;
    private Label statusLabel;

    public GoogleAuthDialog(Stage owner) {
        createDialog(owner);
    }

    private void createDialog(Stage owner) {

        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        // ===== ROOT CONTAINER =====

        VBox mainContainer = new VBox();
        mainContainer.setPrefSize(500, 650);

        mainContainer.setStyle(
                "-fx-background-color: #121212;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0.2, 0, 8);"
        );

        // HEADER
        HBox header = createHeader();

        // ===== PROGRESS =====

        VBox progressContainer = new VBox(8);

        progressContainer.setPadding(new Insets(12,20,12,20));

        progressContainer.setStyle(
                "-fx-background-color: #181818;"
        );

        progressBar = new ProgressBar(0);

        progressBar.setPrefWidth(460);

        progressBar.setStyle(
                "-fx-accent: #1DB954;"
        );

        statusLabel = new Label("Connecting to Google...");

        statusLabel.setStyle(
                "-fx-text-fill: #B3B3B3;" +
                        "-fx-font-size: 11;"
        );

        progressContainer.getChildren().addAll(progressBar,statusLabel);

        // WEBVIEW

        webView = new WebView();

        webEngine = webView.getEngine();

        webView.setPrefSize(500,550);

        webView.setStyle(
                "-fx-background-color: #121212;" +
                        "-fx-background-radius: 0 0 16 16;"
        );

        webEngine.setJavaScriptEnabled(true);

        setupWebViewListeners();

        mainContainer.getChildren().addAll(header,progressContainer,webView);

        StackPane root = new StackPane(mainContainer);

        root.setStyle(
                "-fx-background-color: transparent;"
        );

        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialogStage.setScene(scene);

        // ===== FADE IN ANIMATION =====

        mainContainer.setOpacity(0);

        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(300),
                        mainContainer
                );

        fade.setFromValue(0);

        fade.setToValue(1);

        fade.play();
    }

    private HBox createHeader() {

        HBox header = new HBox();

        header.setAlignment(Pos.CENTER_LEFT);

        header.setPadding(new Insets(16,20,16,20));

        header.setStyle(
                "-fx-background-color: #181818;" +
                        "-fx-background-radius: 16 16 0 0;"
        );

        Label title = new Label("Sign in with Google");

        title.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 16;" +
                        "-fx-font-weight: bold;"
        );

        Region spacer = new Region();

        HBox.setHgrow(spacer,Priority.ALWAYS);

        Button closeBtn = new Button("✕");

        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #B3B3B3;" +
                        "-fx-font-size: 18;" +
                        "-fx-cursor: hand;"
        );

        closeBtn.setOnMouseEntered(e->

                closeBtn.setStyle(
                        "-fx-background-color: #282828;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 18;" +
                                "-fx-background-radius: 50;" +
                                "-fx-cursor: hand;"
                )
        );

        closeBtn.setOnMouseExited(e->

                closeBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #B3B3B3;" +
                                "-fx-font-size: 18;" +
                                "-fx-cursor: hand;"
                )
        );

        closeBtn.setOnAction(e->{

            dialogStage.close();

            if(onError!=null)
                onError.accept(new Exception("Cancelled"));

        });

        header.getChildren().addAll(title,spacer,closeBtn);

        return header;
    }

    private void setupWebViewListeners() {
        // Progress tracking
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            progressBar.setProgress(newVal.doubleValue());
        });

        // Loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Ready");
            } else if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading...");
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load");
                statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #dc2626;");
            }
        });

        // Monitor URL changes - detect redirect with authorization code
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            System.out.println("📍 URL Changed: " + newUrl);

            // Check if redirect URL contains authorization code
            if (newUrl != null && newUrl.contains("localhost:8888/Callback")) {
                System.out.println("✅ Redirect detected!");
                handleRedirect(newUrl);
            }
        });
    }

    private void handleRedirect(String redirectUrl) {
        try {
            // Extract authorization code from URL
            String code = extractCodeFromUrl(redirectUrl);

            if (code != null) {
                System.out.println("🔑 Authorization code received!");
                statusLabel.setText("Authentication successful! Processing...");

                // Hide WebView, show loading
                webView.setVisible(false);

                // Exchange code for user info in background
                new Thread(() -> {
                    try {
                        GoogleAuthService.GoogleUserInfo userInfo =
                                GoogleAuthService.exchangeCodeForUserInfo(code);

                        Platform.runLater(() -> {
                            if (onSuccess != null) {
                                onSuccess.accept(userInfo);
                            }
                            dialogStage.close();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            if (onError != null) {
                                onError.accept(e);
                            }
                            dialogStage.close();
                        });
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (onError != null) {
                onError.accept(e);
            }
            dialogStage.close();
        }
    }

    private String extractCodeFromUrl(String url) {
        try {
            // URL format: http://localhost:8888/Callback?code=XXXXX&scope=...
            if (url.contains("code=")) {
                String[] parts = url.split("code=");
                if (parts.length > 1) {
                    String code = parts[1].split("&")[0];
                    return URLDecoder.decode(code, "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set success callback
     */
    public void setOnSuccess(Consumer<GoogleAuthService.GoogleUserInfo> onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Set error callback
     */
    public void setOnError(Consumer<Exception> onError) {
        this.onError = onError;
    }

    /**
     * Show the dialog and start authentication
     */
    public void show() {
        try {
            // Get authorization URL
            String authUrl = GoogleAuthService.getAuthorizationUrl();

            // Load URL in WebView
            webEngine.load(authUrl);

            // Show dialog
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            if (onError != null) {
                onError.accept(e);
            }
        }
    }
}