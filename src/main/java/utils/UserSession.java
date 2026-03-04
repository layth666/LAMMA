package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserSession {
    private static final Path BASE = Path.of(System.getProperty("user.home"), ".lamma");
    private static final Path USER_FILE = BASE.resolve("user_id.txt");

    public static String getUserId() {
        try {
            if (Files.exists(USER_FILE)) {
                String id = Files.readString(USER_FILE).trim();
                return id.isEmpty() ? null : id;
            }
        } catch (IOException ignored) {}
        return null;
    }

    public static boolean saveUserId(String id) {
        try {
            if (!Files.exists(BASE)) Files.createDirectories(BASE);
            Files.writeString(USER_FILE, id == null ? "" : id);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Impossible d'enregistrer user id: " + e.getMessage());
            return false;
        }
    }
}

