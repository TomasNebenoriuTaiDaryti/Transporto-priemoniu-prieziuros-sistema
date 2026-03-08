package com.example.back.vehicle.vin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class VincarioControlSum {
    private VincarioControlSum() {}

    public static String forVinDecode(String vinUpper, String operationId, String apiKey, String secretKey) {
        String raw = vinUpper + "|" + operationId + "|" + apiKey + "|" + secretKey;
        return sha1Hex(raw).substring(0, 10);
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}