package org.example.algorithm;

//funkcja hashowania bez żadnych gotowych bibliotek
public class SimpleHash {
    public static String hash(String data, String key) {
        String combined = data + key;
        int hash = 0;
        for (char c : combined.toCharArray()) {
            hash += c * 31; // Mnożenie przez liczbę pierwszą dla rozproszenia
        }
        return Integer.toHexString(hash);
    }

    public static boolean verifyHash(String data, String key, String hash) {
        String computedHash = hash(data, key);
        return computedHash.equals(hash);
    }
}
