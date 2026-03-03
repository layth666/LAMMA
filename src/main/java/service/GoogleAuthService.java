package service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

/**
 * Google OAuth 2.0 Authentication Service with WebView
 * Opens an embedded browser window for Google authentication
 */
public class GoogleAuthService {

    private static final String APPLICATION_NAME = "LAMMA";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String REDIRECT_URI = "http://localhost:8888/Callback";

    // Scopes
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "openid"
    );

    private static final String CREDENTIALS_FILE_PATH = "/google_credentials.json";

    private static GoogleAuthorizationCodeFlow flow;
    private static GoogleClientSecrets clientSecrets;

    /**
     * Initialize the OAuth flow
     */
    private static void initializeFlow() throws IOException, GeneralSecurityException {
        if (flow != null) return;

        // Load client secrets
        InputStream in = GoogleAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
    }

    /**
     * Get the authorization URL for WebView
     * ✅ This URL will be loaded in the embedded browser
     */
    public static String getAuthorizationUrl() throws IOException, GeneralSecurityException {
        initializeFlow();

        String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URI)
                .build();

        System.out.println("🔗 Authorization URL: " + authUrl);
        return authUrl;
    }

    /**
     * Exchange authorization code for user information
     * Called after user completes auth in WebView
     */
    public static GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode)
            throws IOException, GeneralSecurityException {

        initializeFlow();

        System.out.println("🔄 Exchanging code for token...");

        // Exchange code for tokens
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(REDIRECT_URI)
                .execute();

        System.out.println("✅ Token received!");

        // Create credential
        Credential credential = flow.createAndStoreCredential(tokenResponse, "user");

        // Get user info
        return getUserInfo(credential);
    }

    /**
     * Get user information from Google
     */
    private static GoogleUserInfo getUserInfo(Credential credential) throws IOException, GeneralSecurityException {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Build HTTP client
        com.google.api.client.http.HttpRequestFactory requestFactory =
                HTTP_TRANSPORT.createRequestFactory(credential);

        // Make request to get user info
        com.google.api.client.http.GenericUrl url = new com.google.api.client.http.GenericUrl(
                "https://www.googleapis.com/oauth2/v2/userinfo"
        );

        com.google.api.client.http.HttpRequest request = requestFactory.buildGetRequest(url);
        com.google.api.client.http.HttpResponse response = request.execute();

        // Parse response
        String jsonResponse = response.parseAsString();

        System.out.println("📧 Raw Response: " + jsonResponse);

        // Parse JSON
        com.google.gson.Gson gson = new com.google.gson.Gson();
        GoogleUserInfo userInfo = gson.fromJson(jsonResponse, GoogleUserInfo.class);

        System.out.println("📧 Email: " + userInfo.getEmail());
        System.out.println("👤 Name: " + userInfo.getName());
        System.out.println("🖼️ Picture: " + userInfo.getPicture());

        return userInfo;
    }

    /**
     * Logout - Clear stored tokens
     */
    public static void logout() {
        try {
            // Delete stored tokens
            java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
            if (tokensDir.exists()) {
                deleteDirectory(tokensDir);
                System.out.println("🚪 Google tokens cleared");
            }
        } catch (Exception e) {
            System.err.println("Error clearing tokens: " + e.getMessage());
        }
    }

    /**
     * Helper method to delete directory recursively
     */
    private static void deleteDirectory(java.io.File directory) {
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    /**
     * Google User Information
     */
    public static class GoogleUserInfo {
        private String id;
        private String email;
        private String name;
        private String picture;
        private String given_name;
        private String family_name;
        private boolean verified_email;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPicture() { return picture; }
        public String getGivenName() { return given_name; }
        public String getFamilyName() { return family_name; }
        public boolean isVerifiedEmail() { return verified_email; }

        public void setId(String id) { this.id = id; }
        public void setEmail(String email) { this.email = email; }
        public void setName(String name) { this.name = name; }
        public void setPicture(String picture) { this.picture = picture; }
        public void setGivenName(String given_name) { this.given_name = given_name; }
        public void setFamilyName(String family_name) { this.family_name = family_name; }
        public void setVerifiedEmail(boolean verified_email) { this.verified_email = verified_email; }

        @Override
        public String toString() {
            return "GoogleUserInfo{" +
                    "id='" + id + '\'' +
                    ", email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    ", picture='" + picture + '\'' +
                    ", verified_email=" + verified_email +
                    '}';
        }
    }
}