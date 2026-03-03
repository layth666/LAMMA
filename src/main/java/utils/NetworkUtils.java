package utils;

import java.net.URL;
import java.net.URLConnection;

/**
 * Utilitaires réseau pour vérifier la connexion internet
 */
public class NetworkUtils {

    /**
     * Vérifie si l'appareil est connecté à Internet
     * @return true si connecté, false sinon
     */
    public static boolean isInternetConnected() {
        try {
            // Test connexion vers Face++ API
            URL url = new URL("https://api-us.faceplusplus.com");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000); // 3 secondes timeout
            connection.connect();
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ No internet connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie la connexion avec message console
     */
    public static boolean checkInternetWithLog() {
        System.out.println("🔍 Checking internet connection...");
        boolean connected = isInternetConnected();
        if (connected) {
            System.out.println("✅ Internet connection OK");
        } else {
            System.out.println("❌ No internet connection");
        }
        return connected;
    }
}