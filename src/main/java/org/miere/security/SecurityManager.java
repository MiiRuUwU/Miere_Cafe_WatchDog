package org.miere.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class SecurityManager {
    public static void init() {
        // We're keeping this simple for now
    }

    //idk how to document this I used AI because I'm a vibe coder
    public static String hashPassword(String password) {
        try {
            // Use SHA-256 because they told me it's the industry standard
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Encode to Base64
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean verifyPassword(String input, String storedHash) {
        String hashedInput = hashPassword(input);
        return hashedInput != null && hashedInput.equals(storedHash);
    }
}