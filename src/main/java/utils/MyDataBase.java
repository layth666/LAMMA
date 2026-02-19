package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection cnx;

    private static final String URL =
            "jdbc:mariadb://127.0.0.1:3307/Gestion_Equipement";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDataBase() {
        try {
            // Charger le driver MariaDB
            Class.forName("org.mariadb.jdbc.Driver");

            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion MariaDB réussie");

        } catch (ClassNotFoundException e) {
            System.out.println("❌ Driver MariaDB introuvable !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("❌ Erreur connexion MariaDB !");
            e.printStackTrace();
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}