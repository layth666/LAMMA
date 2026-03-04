package dao;

import model.Evenement;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementDAO {

    // Ajouter un événement
    public void ajouter(Evenement evenement) throws SQLException {
        // Table unifiée LAMMABD.evenement (structure lamma(2) : id_event, titre, ...)
        String query = "INSERT INTO evenement (titre, date_debut, date_fin, lieu, description, statut) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, evenement.getNom());
            pstmt.setDate(2, Date.valueOf(evenement.getDateDebut()));
            pstmt.setDate(3, Date.valueOf(evenement.getDateFin()));
            pstmt.setString(4, evenement.getLieu());
            pstmt.setString(5, evenement.getDescription());
            pstmt.setString(6, evenement.getStatut());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                evenement.setId(rs.getInt(1));
            }
        }
    }

    // Modifier un événement
    public void modifier(Evenement evenement) throws SQLException {
        String query = "UPDATE evenement SET titre = ?, date_debut = ?, date_fin = ?, lieu = ?, description = ?, statut = ? WHERE id_event = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, evenement.getNom());
            pstmt.setDate(2, Date.valueOf(evenement.getDateDebut()));
            pstmt.setDate(3, Date.valueOf(evenement.getDateFin()));
            pstmt.setString(4, evenement.getLieu());
            pstmt.setString(5, evenement.getDescription());
            pstmt.setString(6, evenement.getStatut());
            pstmt.setInt(7, evenement.getId());

            pstmt.executeUpdate();
        }
    }

    // Supprimer un événement
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM evenement WHERE id_event = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Récupérer tous les événements
    public List<Evenement> listerTous() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement ORDER BY date_debut DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id_event"),
                        rs.getString("titre"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("description"),
                        rs.getString("statut")
                ));
            }
        }
        return evenements;
    }

    // Récupérer les événements à venir
    public List<Evenement> listerAVenir() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement WHERE date_debut >= CURDATE() ORDER BY date_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id_event"),
                        rs.getString("titre"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("description"),
                        rs.getString("statut")
                ));
            }
        }
        return evenements;
    }

    // Récupérer un événement par ID
    public Evenement getById(int id) throws SQLException {
        String query = "SELECT * FROM evenement WHERE id_event = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Evenement(
                        rs.getInt("id_event"),
                        rs.getString("titre"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("description"),
                        rs.getString("statut")
                );
            }
        }
        return null;
    }

    // Récupérer les événements par statut
    public List<Evenement> getByStatut(String statut) throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM evenement WHERE statut = ? ORDER BY date_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, statut);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id_event"),
                        rs.getString("titre"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("description"),
                        rs.getString("statut")
                ));
            }
        }
        return evenements;
    }

    // Compter le nombre total d'événements
    public int compter() throws SQLException {
        String query = "SELECT COUNT(*) FROM evenement";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}