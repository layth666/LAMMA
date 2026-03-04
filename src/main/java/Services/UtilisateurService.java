package Services;

import entities.Utilisateur;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {
    private final Connection cnx;

    public UtilisateurService() {
        cnx = MyDataBase.getInstance().getCnx();
        creerTableSiNexistePas();
    }

    private void creerTableSiNexistePas() {
        String sql = "CREATE TABLE IF NOT EXISTS utilisateur (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            // Insertion d'un admin par défaut si la table est vide
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utilisateur");
            if (rs.next() && rs.getInt(1) == 0) {
                ajouter(new Utilisateur("Alexandre (Admin)", "admin@lamma.com", "admin123" , "admin"));
                ajouter(new Utilisateur("User Test", "user@lamma.com", "user123" , "user"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur création table utilisateur: " + e.getMessage());
        }
    }

    public void ajouter(Utilisateur u) {
        String sql = "INSERT INTO utilisateur (nom, email, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(3, u.getRole());
            ps.executeUpdate();
            System.out.println("✅ Utilisateur ajouté: " + u.getNom());
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout utilisateur");
            e.printStackTrace();
        }
    }

    public Utilisateur login(String email, String password) {
        String sql = "SELECT * FROM utilisateur WHERE email = ? AND password = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Utilisateur getById(int id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
