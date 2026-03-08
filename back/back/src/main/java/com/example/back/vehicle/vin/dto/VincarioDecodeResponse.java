package com.example.back.vehicle.vin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VincarioDecodeResponse {

    @JsonProperty("decode")
    public List<DecodeItem> decode;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecodeItem {
        @JsonProperty("label")
        public String label;

        @JsonProperty("value")
        public Object value;
    }

    public String getString(String... labels) {
        Object v = findFirstValue(labels).orElse(null);
        if (v == null) return null;

        if (v instanceof List<?> list) {
            if (list.isEmpty()) return null;
            Object first = list.get(0);
            return first == null ? null : first.toString();
        }
        return v.toString();
    }

    public Integer getInt(String... labels) {
        Object v = findFirstValue(labels).orElse(null);
        if (v == null) return null;

        if (v instanceof Number n) return n.intValue();

        String s = v.toString().trim();
        if (s.isEmpty()) return null;

        String digits = extractLeadingNumber(s);
        if (digits == null) return null;

        try { return Integer.parseInt(digits); }
        catch (Exception ignored) { return null; }
    }

    public Double getDouble(String... labels) {
        Object v = findFirstValue(labels).orElse(null);
        if (v == null) return null;

        if (v instanceof Number n) return n.doubleValue();

        String s = v.toString().trim();
        if (s.isEmpty()) return null;

        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Optional<Object> findFirstValue(String... labels) {
        if (decode == null || labels == null || labels.length == 0) return Optional.empty();

        for (String wanted : labels) {
            if (wanted == null) continue;
            String wantedNorm = normalize(wanted);

            for (DecodeItem i : decode) {
                if (i == null || i.label == null) continue;
                if (normalize(i.label).equals(wantedNorm)) {
                    if (i.value != null) return Optional.of(i.value);
                }
            }
        }
        return Optional.empty();
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static String extractLeadingNumber(String s) {
        StringBuilder b = new StringBuilder();
        boolean started = false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                b.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        return b.isEmpty() ? null : b.toString();
    }

    public String vin() {
        return getString("VIN");
    }
    public String make() {
        return getString("Make");
    }
    public String model() {
        return getString("Model");
    }
    public Integer modelYear() {
        return getInt("Model Year", "Year", "Model year");
    }
    public String manufacturer() {
        return getString("Manufacturer");
    }
    public String plantCountry() {
        return getString("Plant Country");
    }
    public String body() {
        return getString("Body");
    }
    public String drive() {
        return getString("Drive");
    }

    public String transmission() {
        String t = getString("Transmission");
        if (t != null) return t;
        return getString("Transmission (full)");
    }

    public String fuelType() {
        String f = getString("Fuel Type - Primary");
        if (f != null) return f;

        f = getString("Fuel Type");
        if (f != null) return f;

        return getString("Fuel Type - Secondary", "Fuel");
    }

    public Integer doors() {
        return getInt("Number of Doors");
    }
    public Integer seats() {
        return getInt("Number of Seats");
    }

    public Double co2Gkm() {
        Double v = getDouble("Average CO2 Emission (g/km)");
        if (v != null) return v;
        return getDouble("CO2 Emission (g/km)", "CO2 Emission (g/km) (WLTP)");
    }

    public Integer engineCylinders() {
        return getInt("Engine Cylinders");
    }
    public Integer engineDisplacementCc() {
        Integer cc = getInt(
                "Engine Displacement (ccm)",
                "Engine Displacement (cc)",
                "Engine Displacement (ccm.)",
                "Engine Displacement (cm3)",
                "Engine Displacement (ccm³)",
                "Engine Displacement (ccm3)"
        );
        if (cc != null) return cc;

        Double liters = getDouble("Engine Displacement (l)", "Engine Displacement (L)");
        if (liters != null) return (int) Math.round(liters * 1000);

        String engineFull = getString("Engine (full)");
        if (engineFull != null) {
            Double l = extractLeadingDouble(engineFull);
            if (l != null) return (int) Math.round(l * 1000);
        }
        return null;
    }

    public String engineFull() {
        return getString("Engine (full)");
    }
    public Integer enginePowerKw() {
        return getInt("Engine Power (kW)");
    }

    private static Double extractLeadingDouble(String s) {
        StringBuilder b = new StringBuilder();
        boolean started = false;
        for (char c : s.trim().toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == ',') {
                b.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        if (b.isEmpty()) return null;
        try {
            return Double.parseDouble(b.toString().replace(",", "."));
        } catch (Exception ignored) {
            return null;
        }
    }
}