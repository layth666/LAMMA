package service;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Service Kairos Face Recognition
 * API professionnelle de reconnaissance faciale
 */
public class KairosService {

    // ⚠️ REMPLACEZ PAR VOS VRAIES CLÉS KAIROS
    private static final String APP_ID = "89590aed";
    private static final String APP_KEY = "15c16d3cd58977d65fb65a279cd28967";
    private static final String API_URL = "https://api.kairos.com";
    private static final String GALLERY_NAME = "event_platform_users";

    /**
     * Enroller un visage (première inscription)
     */
    public KairosResponse enrollFace(File imageFile, String subjectId) {
        try {
            System.out.println("📤 Enrolling face for subject: " + subjectId);

            String imageBase64 = encodeImageToBase64(imageFile);

            JSONObject requestBody = new JSONObject();
            requestBody.put("image", imageBase64);
            requestBody.put("subject_id", subjectId);
            requestBody.put("gallery_name", GALLERY_NAME);

            String response = makeRequest("/enroll", requestBody.toString());
            JSONObject jsonResponse = new JSONObject(response);

            System.out.println("✅ Enrollment response: " + jsonResponse.toString());

            if (jsonResponse.has("images")) {
                JSONArray images = jsonResponse.getJSONArray("images");
                if (images.length() > 0) {
                    JSONObject firstImage = images.getJSONObject(0);
                    String faceId = firstImage.optString("face_id", null);

                    return new KairosResponse(true, "Face enrolled successfully", faceId, subjectId, 100.0);
                }
            }

            return new KairosResponse(false, "Enrollment failed", null, null, 0.0);

        } catch (Exception e) {
            System.err.println("❌ Enrollment error: " + e.getMessage());
            e.printStackTrace();
            return new KairosResponse(false, "Error: " + e.getMessage(), null, null, 0.0);
        }
    }

    /**
     * Reconnaître un visage (login)
     */
    public KairosResponse recognizeFace(File imageFile) {
        try {
            System.out.println("🔍 Recognizing face...");

            String imageBase64 = encodeImageToBase64(imageFile);

            JSONObject requestBody = new JSONObject();
            requestBody.put("image", imageBase64);
            requestBody.put("gallery_name", GALLERY_NAME);
            requestBody.put("threshold", "0.60"); // 60% minimum confidence

            String response = makeRequest("/recognize", requestBody.toString());
            JSONObject jsonResponse = new JSONObject(response);

            System.out.println("📊 Recognition response: " + jsonResponse.toString());

            if (jsonResponse.has("images")) {
                JSONArray images = jsonResponse.getJSONArray("images");
                if (images.length() > 0) {
                    JSONObject firstImage = images.getJSONObject(0);

                    if (firstImage.has("transaction")) {
                        JSONObject transaction = firstImage.getJSONObject("transaction");

                        if (transaction.has("subject_id") && transaction.has("confidence")) {
                            String subjectId = transaction.getString("subject_id");
                            double confidence = transaction.getDouble("confidence");
                            String faceId = transaction.optString("face_id", null);

                            System.out.println("✅ Match found: " + subjectId + " (Confidence: " + confidence + "%)");

                            return new KairosResponse(true, "Face recognized", faceId, subjectId, confidence);
                        }
                    }
                }
            }

            System.out.println("⚠️ No match found");
            return new KairosResponse(false, "No match found", null, null, 0.0);

        } catch (Exception e) {
            System.err.println("❌ Recognition error: " + e.getMessage());
            e.printStackTrace();
            return new KairosResponse(false, "Error: " + e.getMessage(), null, null, 0.0);
        }
    }

    /**
     * Vérifier si un subject existe déjà
     */
    public boolean subjectExists(String subjectId) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("gallery_name", GALLERY_NAME);

            String response = makeRequest("/gallery/view", requestBody.toString());
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("subject_ids")) {
                JSONArray subjects = jsonResponse.getJSONArray("subject_ids");
                for (int i = 0; i < subjects.length(); i++) {
                    if (subjects.getString(i).equals(subjectId)) {
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("❌ Error checking subject: " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprimer un visage
     */
    public boolean removeFace(String subjectId) {
        try {
            System.out.println("🗑️ Removing face for subject: " + subjectId);

            JSONObject requestBody = new JSONObject();
            requestBody.put("subject_id", subjectId);
            requestBody.put("gallery_name", GALLERY_NAME);

            String response = makeRequest("/gallery/remove_subject", requestBody.toString());
            System.out.println("✅ Remove response: " + response);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Remove error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lister tous les subjects dans la gallery
     */
    public JSONArray listAllSubjects() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("gallery_name", GALLERY_NAME);

            String response = makeRequest("/gallery/view", requestBody.toString());
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("subject_ids")) {
                return jsonResponse.getJSONArray("subject_ids");
            }

            return new JSONArray();

        } catch (Exception e) {
            System.err.println("❌ List error: " + e.getMessage());
            return new JSONArray();
        }
    }

    /**
     * Encoder une image en Base64
     */
    private String encodeImageToBase64(File imageFile) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Faire une requête à l'API Kairos
     */
    private String makeRequest(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(API_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("app_id", APP_ID);
        conn.setRequestProperty("app_key", APP_KEY);
        conn.setDoOutput(true);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = conn.getResponseCode();

        InputStream is = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

        if (is == null) {
            throw new IOException("Failed to get response stream");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String result = response.toString();
            System.out.println("🔥 RAW KAIROS RESPONSE:");
            System.out.println(result);
            return result;
        }
    }

    /**
     * Classe de réponse Kairos
     */
    public static class KairosResponse {
        public final boolean success;
        public final String message;
        public final String faceId;
        public final String subjectId;
        public final double confidence;

        public KairosResponse(boolean success, String message, String faceId, String subjectId, double confidence) {
            this.success = success;
            this.message = message;
            this.faceId = faceId;
            this.subjectId = subjectId;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "KairosResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", subjectId='" + subjectId + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}