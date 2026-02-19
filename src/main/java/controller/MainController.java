package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

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

    private Parent sponsorView;
    private Parent eventSponsorView;

    @FXML
    public void initialize() {
        try {
            sponsorView = FXMLLoader.load(getClass().getResource("/view/SponsorView.fxml"));
            eventSponsorView = FXMLLoader.load(getClass().getResource("/view/EventSponsorView.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        showSponsors();

        btnSponsors.setOnAction(e -> showSponsors());
        btnEventSponsor.setOnAction(e -> showEventSponsor());

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

    private void setActiveButton(Button active) {
        btnSponsors.getStyleClass().remove("active");
        btnEventSponsor.getStyleClass().remove("active");
        active.getStyleClass().add("active");
    }
}
