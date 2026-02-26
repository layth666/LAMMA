//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/reddit_simple";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/reddit_simple", "root", "");
    }
}
