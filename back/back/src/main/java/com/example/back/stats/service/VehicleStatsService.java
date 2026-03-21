package com.example.back.stats.service;

import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.stats.dto.VehicleStatsResponse;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VehicleStatsService {

    private final VehicleAccessService access;
    private final ServiceRecordRepo records;
    private final VehicleDocumentRepo documents;
    private final ObjectMapper om;

    public VehicleStatsService(
            VehicleAccessService access,
            ServiceRecordRepo records,
            VehicleDocumentRepo documents,
            ObjectMapper om
    ) {
        this.access = access;
        this.records = records;
        this.documents = documents;
        this.om = om;
    }

    public VehicleStatsResponse stats(Long userId, Long vehicleId) {
        var vehicle = access.getVehicleOrThrow(vehicleId);
        if (!access.canView(userId, vehicle)) {
            throw new IllegalArgumentException("Neturi prieigos prie šios mašinos");
        }

        String vehicleName = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getModelYear() + ")";

        Map<String, BigDecimal> monthly = new TreeMap<>();
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        List<VehicleStatsResponse.FuelHistoryPoint> fuel = new ArrayList<>();
        Map<String, BigDecimal> fuelCostByMonth = new TreeMap<>();
        Map<String, BigDecimal> fuelLitersByMonth = new TreeMap<>();

        var recs = records.findAllByVehicleIdOrderByPerformedAtDesc(vehicleId);
        for (var r : recs) {
            String month = r.getPerformedAt().atZone(ZoneId.of("Europe/Vilnius")).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            BigDecimal cost = r.getTotalCost() == null ? BigDecimal.ZERO : r.getTotalCost();

            monthly.put(month, monthly.getOrDefault(month, BigDecimal.ZERO).add(cost));

            String typeKey = switch (r.getKind()) {
                case "FUEL" -> "Degalai";
                case "SERVICE" -> "Servisas";
                case "OIL" -> "Tepalai";
                case "BRAKES" -> "Stabdžiai";
                case "TIRES" -> "Padangos";
                default -> "Kita";
            };
            byType.put(typeKey, byType.getOrDefault(typeKey, BigDecimal.ZERO).add(cost));

            if ("FUEL".equals(r.getKind())) {
                BigDecimal liters = BigDecimal.ZERO;
                try {
                    JsonNode n = om.readTree(r.getMetaJson());
                    if (n.hasNonNull("liters")) {
                        liters = n.get("liters").decimalValue();
                    }
                } catch (Exception ignored) {}

                BigDecimal ppl = BigDecimal.ZERO;
                if (liters.compareTo(BigDecimal.ZERO) > 0) {
                    ppl = cost.divide(liters, 2, RoundingMode.HALF_UP);
                }

                String date = r.getPerformedAt().atZone(ZoneId.of("Europe/Vilnius")).toLocalDate().toString();
                fuel.add(new VehicleStatsResponse.FuelHistoryPoint(date, liters, cost, ppl));

                fuelCostByMonth.put(month, fuelCostByMonth.getOrDefault(month, BigDecimal.ZERO).add(cost));
                fuelLitersByMonth.put(month, fuelLitersByMonth.getOrDefault(month, BigDecimal.ZERO).add(liters));
            }
        }

        var docs = documents.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId);
        for (var d : docs) {
            BigDecimal docCost = BigDecimal.ZERO;
            try {
                JsonNode n = om.readTree(d.getMetaJson());
                if (n.hasNonNull("price")) {
                    docCost = n.get("price").decimalValue();
                } else if (n.hasNonNull("amount")) {
                    docCost = n.get("amount").decimalValue();
                }
            } catch (Exception ignored) {}

            if (docCost.compareTo(BigDecimal.ZERO) > 0) {
                String month = d.getCreatedAt().atZone(ZoneId.of("Europe/Vilnius")).format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthly.put(month, monthly.getOrDefault(month, BigDecimal.ZERO).add(docCost));

                String typeKey = switch (d.getType()) {
                    case "INSURANCE" -> "Draudimas";
                    case "INSPECTION" -> "Techninė";
                    case "FINE" -> "Baudos";
                    default -> "Dokumentai";
                };
                byType.put(typeKey, byType.getOrDefault(typeKey, BigDecimal.ZERO).add(docCost));
            }
        }
        fuel.sort(Comparator.comparing(VehicleStatsResponse.FuelHistoryPoint::date));

        List<VehicleStatsResponse.MonthlyCostPoint> monthlyPoints = monthly.entrySet().stream().map(e -> new VehicleStatsResponse.MonthlyCostPoint(e.getKey(), e.getValue())).toList();
        List<VehicleStatsResponse.ExpenseByTypePoint> byTypePoints = byType.entrySet().stream().map(e -> new VehicleStatsResponse.ExpenseByTypePoint(e.getKey(), e.getValue())).toList();
        List<VehicleStatsResponse.MonthlyFuelAveragePoint> monthlyFuelAverages = new ArrayList<>();
        for (var entry : fuelCostByMonth.entrySet()) {
            String month = entry.getKey();
            BigDecimal totalCost = entry.getValue();
            BigDecimal totalLiters = fuelLitersByMonth.getOrDefault(month, BigDecimal.ZERO);

            BigDecimal avg = BigDecimal.ZERO;
            if (totalLiters.compareTo(BigDecimal.ZERO) > 0) {
                avg = totalCost.divide(totalLiters, 3, RoundingMode.HALF_UP);
            }

            monthlyFuelAverages.add(new VehicleStatsResponse.MonthlyFuelAveragePoint(month, avg));
        }

        return new VehicleStatsResponse(vehicleId, vehicleName, monthlyPoints, byTypePoints, fuel, monthlyFuelAverages);
    }
}
