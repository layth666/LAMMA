package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {

    @FXML
    private HBox headerBox;
    @FXML
    private StackPane contentPane;
    @FXML
    private Button btnSponsors;
    @FXML
    private Button btnEventSponsor;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnSideDashboard;
    @FXML
    private Button btnSideSponsors;
    @FXML
    private Button btnSideEventSponsor;
    @FXML
    private Button btnSideStats;
    @FXML
    private VBox boxSponsorSubMenu;

    private Parent sponsorView;
    private Parent eventSponsorView;
    private Parent dashboardView;
    private DashboardController dashboardController;

    @FXML
    public void initialize() {
        try {
            sponsorView = FXMLLoader.load(getClass().getResource("/view/SponsorView.fxml"));
            eventSponsorView = FXMLLoader.load(getClass().getResource("/view/EventSponsorView.fxml"));

            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/view/DashboardView.fxml"));
            dashboardView = dashboardLoader.load();
            dashboardController = dashboardLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        showSponsors();

        btnSponsors.setOnAction(e -> showSponsors());
        btnEventSponsor.setOnAction(e -> showEventSponsor());
        btnDashboard.setOnAction(e -> showDashboard());

        if (btnSideDashboard != null) {
            btnSideDashboard.setOnAction(e -> showDashboard());
        }
        if (btnSideSponsors != null) {
            btnSideSponsors.setOnAction(e -> {
                showSponsors();
                toggleSponsorSubMenu();
            });
        }
        if (btnSideEventSponsor != null) {
            btnSideEventSponsor.setOnAction(e -> showEventSponsor());
        }
        if (btnSideStats != null) {
            btnSideStats.setOnAction(e -> showDashboard());
        }

        setActiveButton(btnSponsors);
    }

    private void showSponsors() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(sponsorView);
        setActiveButton(btnSponsors);
    }

    private void showEventSponsor() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(eventSponsorView);
        setActiveButton(btnEventSponsor);
    }

    private void showDashboard() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(dashboardView);
        if (dashboardController != null) {
            dashboardController.actualiser();
        }
        setActiveButton(btnDashboard);
    }

    private void toggleSponsorSubMenu() {
        if (boxSponsorSubMenu == null) return;
        boolean visible = !boxSponsorSubMenu.isVisible();
        boxSponsorSubMenu.setVisible(visible);
        boxSponsorSubMenu.setManaged(visible);
    }

    private void setActiveButton(Button active) {
        btnSponsors.getStyleClass().remove("active");
        btnEventSponsor.getStyleClass().remove("active");
        if (btnDashboard != null) {
            btnDashboard.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }
}
