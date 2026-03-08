package com.example.back.vehicle.service;

import java.util.Locale;
public final class FuelTypeMapper {
    private FuelTypeMapper() {}

    public static String normalizeToDbCode(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);

        if (s.contains("diesel")) return "DIESEL";
        if (s.contains("gasoline") || s.contains("petrol")) return "PETROL";
        if (s.contains("electric")) return "EV";
        if (s.contains("lpg")) return "LPG";
        if (s.contains("cng")) return "CNG";
        if (s.contains("plug") || s.contains("phev")) return "PHEV";
        if (s.contains("hybrid")) return "HYBRID";
        return "OTHER";
    }
}