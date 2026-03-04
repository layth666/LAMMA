package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ClientIdUtil {
    private static final String FNAME = "client_id.txt";

    public static String getClientId() {
        try {
            Path p = Path.of(System.getProperty("user.home"), ".lamma");
            if (!Files.exists(p)) Files.createDirectories(p);
            Path f = p.resolve(FNAME);
            if (Files.exists(f)) {
                return Files.readString(f).trim();
            } else {
                String id = UUID.randomUUID().toString();
                Files.writeString(f, id);
                return id;
            }
        } catch (IOException e) {
            // fallback
            return UUID.randomUUID().toString();
        }
    }
}

