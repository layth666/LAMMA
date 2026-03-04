package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Configuration de votre base de données unifiée
    // Nouvelle base : LAMMABD (fusion des anciennes bases)
    private static final String URL = "jdbc:mysql://localhost:3306/LAMMABD";
    private static final String USER = "root";     // Votre utilisateur MySQL
    private static final String PASSWORD = "";     // Votre mot de passe MySQL

    // Chargement du driver MySQL
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ Driver MySQL chargé avec succès");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Erreur: Driver MySQL non trouvé !");
            e.printStackTrace();
        }
    }

    // Méthode pour obtenir une connexion
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Connexion à la base de données établie");
            return conn;
        } catch (SQLException e) {
            System.err.println("✗ Erreur de connexion à la base de données !");
            System.err.println("  URL: " + URL);
            System.err.println("  User: " + USER);
            throw e;
        }
    }

    // Méthode pour tester la connexion
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✓ Test de connexion réussi !");
            System.out.println("  Base de données: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("✗ Échec du test de connexion !");
            e.printStackTrace();
        }
    }

    // Méthode pour fermer la connexion
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("✓ Connexion fermée");
            } catch (SQLException e) {
                System.err.println("✗ Erreur lors de la fermeture de la connexion");
                e.printStackTrace();
            }
        }
    }
}