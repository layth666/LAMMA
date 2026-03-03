package service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;

/**
 * Cloudinary Image Upload Service
 * Handles image uploads to Cloudinary cloud storage
 */
public class CloudinaryService {

    private static Cloudinary cloudinary;

    // ✅ CONFIGURATION - Remplacez avec vos credentials Cloudinary
    private static final String CLOUD_NAME = "dmyesvgdy";
    private static final String API_KEY = "686757755912422";
    private static final String API_SECRET = "_kyfjN16J_VxXpARRDgCRP4HhFo";

    /**
     * Initialize Cloudinary
     */
    private static void initCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", CLOUD_NAME,
                    "api_key", API_KEY,
                    "api_secret", API_SECRET
            ));

            System.out.println("☁️ Cloudinary initialized");
        }
    }

    /**
     * Upload image to Cloudinary
     * @param imageFile The image file to upload
     * @return The secure URL of the uploaded image
     * @throws Exception If upload fails
     */
    public static String uploadImage(File imageFile) throws Exception {
        initCloudinary();

        System.out.println("📤 Uploading image to Cloudinary: " + imageFile.getName());

        // Upload with options
        Map uploadResult = cloudinary.uploader().upload(imageFile, ObjectUtils.asMap(
                "folder", "lamma/profiles",  // Folder in Cloudinary
                "resource_type", "image",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false
        ));

        // Get secure URL
        String imageUrl = (String) uploadResult.get("secure_url");

        System.out.println("✅ Image uploaded successfully!");
        System.out.println("🔗 URL: " + imageUrl);

        return imageUrl;
    }

    /**
     * Upload image with custom public ID
     * @param imageFile The image file to upload
     * @param publicId Custom public ID for the image
     * @return The secure URL of the uploaded image
     * @throws Exception If upload fails
     */
    public static String uploadImageWithId(File imageFile, String publicId) throws Exception {
        initCloudinary();

        System.out.println("📤 Uploading image with ID: " + publicId);

        Map uploadResult = cloudinary.uploader().upload(imageFile, ObjectUtils.asMap(
                "folder", "lamma/profiles",
                "public_id", publicId,
                "resource_type", "image",
                "overwrite", true
        ));

        String imageUrl = (String) uploadResult.get("secure_url");

        System.out.println("✅ Image uploaded: " + imageUrl);

        return imageUrl;
    }

    /**
     * Delete image from Cloudinary
     * @param publicId The public ID of the image to delete
     * @throws Exception If deletion fails
     */
    public static void deleteImage(String publicId) throws Exception {
        initCloudinary();

        System.out.println("🗑️ Deleting image: " + publicId);

        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        System.out.println("✅ Image deleted");
    }

    /**
     * Extract public ID from Cloudinary URL
     * @param imageUrl The Cloudinary URL
     * @return The public ID
     */
    public static String extractPublicId(String imageUrl) {
        // URL format: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/lamma/profiles/image.jpg
        if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
            String[] parts = imageUrl.split("/");
            // Get the folder path + filename without extension
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("upload")) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex != -1 && uploadIndex + 2 < parts.length) {
                // Skip version (v1234567890) if present
                int startIndex = uploadIndex + 1;
                if (parts[startIndex].startsWith("v")) {
                    startIndex++;
                }

                // Build public ID
                StringBuilder publicId = new StringBuilder();
                for (int i = startIndex; i < parts.length; i++) {
                    publicId.append(parts[i]);
                    if (i < parts.length - 1) {
                        publicId.append("/");
                    }
                }

                // Remove file extension
                String result = publicId.toString();
                int dotIndex = result.lastIndexOf(".");
                if (dotIndex != -1) {
                    result = result.substring(0, dotIndex);
                }

                return result;
            }
        }
        return null;
    }
}