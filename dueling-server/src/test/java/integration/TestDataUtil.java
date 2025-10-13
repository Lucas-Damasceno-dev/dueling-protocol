package integration;

import java.util.UUID;

public class TestDataUtil {
    
    public static String generateUniqueUsername(String baseName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return baseName + "_" + uuid;
    }
    
    public static String generateUniquePlayerId(String baseId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return baseId + "_" + uuid;
    }
    
    public static String generateUniquePassword() {
        return "password_" + UUID.randomUUID().toString().substring(0, 8);
    }
}