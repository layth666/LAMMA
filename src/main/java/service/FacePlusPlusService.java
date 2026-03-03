package service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service Face++ pour reconnaissance faciale
 * Utilise l'API Face++ (anciennement Megvii)
 */
public class FacePlusPlusService {

    // VOS CLÉS FACE++ (déjà configurées)
    private static final String API_KEY = "QG-Vxra4bljoUjxuUyd_3dcfw7_xO_lI";
    private static final String API_SECRET = "-HqFuhqijHdUOBHTD4QTUTaQlhYAXKXE";
    private static final String COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    /**
     * Compare deux images de visages
     * @param image1 Première image (celle capturée)
     * @param image2 Deuxième image (celle enregistrée)
     * @return Score de confiance (0-100), ou -1 si erreur
     */
    public double compareFaces(File image1, File image2) {
        if (!image1.exists() || !image2.exists()) {
            System.err.println("❌ One or both image files do not exist");
            System.err.println("   Image 1: " + image1.getAbsolutePath() + " (exists: " + image1.exists() + ")");
            System.err.println("   Image 2: " + image2.getAbsolutePath() + " (exists: " + image2.exists() + ")");
            return -1.0;
        }

        System.out.println("📊 Comparing faces with Face++...");
        System.out.println("   Image 1: " + image1.getName());
        System.out.println("   Image 2: " + image2.getName());

        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(COMPARE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                // API Key
                addFormField(out, "api_key", API_KEY, boundary);
                // API Secret
                addFormField(out, "api_secret", API_SECRET, boundary);

                // Images
                addFilePart(out, "image_file1", image1, boundary);
                addFilePart(out, "image_file2", image2, boundary);

                // End boundary
                out.writeBytes("--" + boundary + "--\r\n");
                out.flush();
            }

            int responseCode = conn.getResponseCode();
            BufferedReader in;

            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                System.err.println("❌ Face++ API Error Response Code: " + responseCode);
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            String jsonResponse = response.toString();
            System.out.println("📥 Face++ Response: " + jsonResponse);

            if (responseCode == 200) {
                // Parse confidence
                int confidenceIndex = jsonResponse.indexOf("\"confidence\":");
                if (confidenceIndex != -1) {
                    int commaIndex = jsonResponse.indexOf(",", confidenceIndex);
                    int bracketIndex = jsonResponse.indexOf("}", confidenceIndex);
                    int endIndex = (commaIndex != -1 && commaIndex < bracketIndex) ? commaIndex : bracketIndex;

                    String scoreStr = jsonResponse.substring(confidenceIndex + 13, endIndex).trim();
                    double confidence = Double.parseDouble(scoreStr);

                    System.out.println("✅ Face++ Confidence: " + confidence + "%");
                    return confidence;

                } else if (jsonResponse.contains("\"faces1\":[]")) {
                    System.err.println("❌ No face detected in captured image");
                    return -2.0;
                } else if (jsonResponse.contains("\"faces2\":[]")) {
                    System.err.println("❌ No face detected in registered image");
                    return -3.0;
                } else {
                    System.err.println("❌ Confidence score not found in response");
                }
            } else {
                System.err.println("❌ Face++ API Error Body: " + jsonResponse);
            }

        } catch (Exception e) {
            System.err.println("❌ Exception during face comparison: " + e.getMessage());
            e.printStackTrace();
        }

        return -1.0;
    }

    private void addFormField(DataOutputStream out, String name, String value, String boundary) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.writeBytes(value + "\r\n");
    }

    private void addFilePart(DataOutputStream out, String fieldName, File file, String boundary) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"\r\n");
        out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.writeBytes("\r\n");
    }

    /**
     * Classe de résultat Face++
     */
    public static class FaceCompareResult {
        public final boolean success;
        public final double confidence;
        public final String message;

        public FaceCompareResult(boolean success, double confidence, String message) {
            this.success = success;
            this.confidence = confidence;
            this.message = message;
        }

        @Override
        public String toString() {
            return "FaceCompareResult{success=" + success + ", confidence=" + confidence + "%, message='" + message + "'}";
        }
    }
}