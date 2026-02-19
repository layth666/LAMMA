package dao;

import model.Evenement;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementDAO {

    // Ajouter un événement
    public void ajouter(Evenement evenement) throws SQLException {
        String query = "INSERT INTO Evenement (nom, date_debut, date_fin, lieu, description, statut) VALUES (?, ?, ?, ?, ?, ?)";
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
        String query = "UPDATE Evenement SET nom = ?, date_debut = ?, date_fin = ?, lieu = ?, description = ?, statut = ? WHERE id = ?";
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
        String query = "DELETE FROM Evenement WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Récupérer tous les événements
    public List<Evenement> listerTous() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT * FROM Evenement ORDER BY date_debut DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
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
        String query = "SELECT * FROM Evenement WHERE date_debut >= CURDATE() ORDER BY date_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
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
        String query = "SELECT * FROM Evenement WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
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
        String query = "SELECT * FROM Evenement WHERE statut = ? ORDER BY date_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, statut);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                evenements.add(new Evenement(
                        rs.getInt("id"),
                        rs.getString("nom"),
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
        String query = "SELECT COUNT(*) FROM Evenement";
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