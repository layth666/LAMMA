package controller;

import dao.EventSponsorDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import model.EventStats;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label lblLastUpdate;
    @FXML private Label lblTotalMontant;
    @FXML private Label lblNbAssociations;
    @FXML private Label lblTopEvent;
    @FXML private PieChart pieParEvenement;

    private final EventSponsorDAO eventSponsorDAO = new EventSponsorDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerStats();
    }

    /** Appelé à chaque affichage du Dashboard pour rafraîchir les données (ex. après ajout d'une association). */
    public void actualiser() {
        chargerStats();
    }

    private void chargerStats() {
        try {
            List<EventStats> stats = eventSponsorDAO.getTotalsParEvenement();
            if (stats.isEmpty()) {
                lblTotalMontant.setText("Aucune donnée d'association.");
                lblNbAssociations.setText("");
                lblTopEvent.setText("");
                return;
            }

            double totalGlobal = stats.stream().mapToDouble(EventStats::getTotalMontant).sum();
            int nbAssociations = stats.stream().mapToInt(EventStats::getNbAssociations).sum();

            lblTotalMontant.setText(String.format("Total global collecté : %.2f DT", totalGlobal));
            lblNbAssociations.setText(String.format("Nombre total d'associations : %d", nbAssociations));

            EventStats top = stats.get(0);
            lblTopEvent.setText(String.format("Top événement : %s (%.2f DT, %d associations)",
                    top.getNomEvenement(), top.getTotalMontant(), top.getNbAssociations()));

            // PieChart
            pieParEvenement.getData().clear();
            for (EventStats es : stats) {
                PieChart.Data slice = new PieChart.Data(es.getNomEvenement(), es.getTotalMontant());
                pieParEvenement.getData().add(slice);
            }

            // Interaction : afficher détail au survol du camembert
            for (PieChart.Data data : pieParEvenement.getData()) {
                data.getNode().setOnMouseEntered(e -> {
                    double value = data.getPieValue();
                    double pourcentage = (value / totalGlobal) * 100.0;
                    lblTopEvent.setText(String.format("%s : %.2f DT (%.1f%% du total)",
                            data.getName(), value, pourcentage));
                });
            }

            lblLastUpdate.setText("Dernière mise à jour : " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur Dashboard");
            alert.setHeaderText("Impossible de charger les statistiques");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}

