package org.example.algorithm;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//funkcja hashowania wykorzystująca MessageDigest, oparta o SHA-256
public class CustomHash {
    public static String hash(String data, String key) {
        try {
            String combined = data + key;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(combined.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static boolean verifyHash(String data, String key, String hash) {
        String computedHash = hash(data, key);
        return computedHash.equals(hash);
    }
}
