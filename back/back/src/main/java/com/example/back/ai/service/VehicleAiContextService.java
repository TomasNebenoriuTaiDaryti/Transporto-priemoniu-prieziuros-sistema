package com.example.back.ai.service;

import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.record.repo.ServiceRecordRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.back.vehicle.service.VehicleAccessService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VehicleAiContextService {

    private final VehicleAccessService vehicleAccessService;
    private final ServiceRecordRepo recordRepository;
    private final VehicleDocumentRepo documentRepository;
    private final ObjectMapper objectMapper;

    public VehicleAiContextService(VehicleAccessService vehicleAccessService, ServiceRecordRepo recordRepository, VehicleDocumentRepo documentRepository, ObjectMapper objectMapper) {
        this.vehicleAccessService = vehicleAccessService;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    public String buildSanitizedContext(Long userId, Long vehicleId) {
        var vehicle = vehicleAccessService.getVehicleOrThrow(vehicleId);
        if (!vehicleAccessService.canView(userId, vehicle)) {
            throw new IllegalArgumentException("Neturi prieigos prie šios mašinos");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("AUTOMOBILIO KONTEKSTAS\n");
        sb.append("Svarbu: šiame kontekste nėra asmens duomenų ir nėra VIN.\n\n");

        sb.append("Techniniai duomenys:\n");
        sb.append("- Markė: ").append(nullSafe(vehicle.getMake())).append("\n");
        sb.append("- Modelis: ").append(nullSafe(vehicle.getModel())).append("\n");
        sb.append("- Metai: ").append(nullSafe(vehicle.getModelYear())).append("\n");
        sb.append("- Variklio darbinis tūris ccm: ").append(nullSafe(vehicle.getEngineDisplacementCc())).append("\n");
        sb.append("- Kuras: ").append(nullSafe(vehicle.getFuelType())).append("\n");
        sb.append("- Pavarų dėžė: ").append(nullSafe(vehicle.getTransmission())).append("\n");
        sb.append("- Varantieji ratai: ").append(nullSafe(vehicle.getDrive())).append("\n");
        sb.append("- Kėbulas: ").append(nullSafe(vehicle.getBody())).append("\n");
        sb.append("- Durys: ").append(nullSafe(vehicle.getDoors())).append("\n");
        sb.append("- Vietos: ").append(nullSafe(vehicle.getSeats())).append("\n");
        sb.append("- CO2 g/km: ").append(nullSafe(vehicle.getCo2Gkm())).append("\n");
        sb.append("- Gamintojo šalis: ").append(nullSafe(vehicle.getPlantCountry())).append("\n");
        sb.append("- Gamintojas: ").append(nullSafe(vehicle.getManufacturer())).append("\n");
        sb.append("- Dabartinė rida km: ").append(nullSafe(vehicle.getOdometerKm())).append("\n\n");

        BigDecimal totalRecordCost = BigDecimal.ZERO;
        BigDecimal totalDocumentCost = BigDecimal.ZERO;
        BigDecimal totalFuelCost = BigDecimal.ZERO;
        BigDecimal totalFuelLiters = BigDecimal.ZERO;

        Map<String, Integer> recordTypeCounts = new LinkedHashMap<>();
        Map<String, Integer> docTypeCounts = new LinkedHashMap<>();

        var records = recordRepository.findAllByVehicleIdOrderByPerformedAtDesc(vehicleId);
        sb.append("Įrašai (naujausi pirmi, iki 20):\n");
        int recordLimit = 0;
        for (var r : records) {
            BigDecimal cost = r.getTotalCost() == null ? BigDecimal.ZERO : r.getTotalCost();
            totalRecordCost = totalRecordCost.add(cost);
            recordTypeCounts.put(r.getKind(), recordTypeCounts.getOrDefault(r.getKind(), 0) + 1);

            String date = r.getPerformedAt() == null ? "-" : r.getPerformedAt().atZone(ZoneId.of("Europe/Vilnius")).toLocalDate().toString();

            BigDecimal liters = null;
            try {
                JsonNode meta = objectMapper.readTree(r.getMetaJson() == null ? "{}" : r.getMetaJson());
                if (meta.hasNonNull("liters")) {
                    liters = meta.get("liters").decimalValue();
                }
            } catch (Exception ignored) {}

            if ("FUEL".equals(r.getKind())) {
                totalFuelCost = totalFuelCost.add(cost);
                if (liters != null) {
                    totalFuelLiters = totalFuelLiters.add(liters);
                }
            }

            if (recordLimit < 20) {
                sb.append("- [").append(r.getKind()).append("] data=").append(date).append(", pavadinimas=").append(nullSafe(r.getTitle())).append(", rida=").append(nullSafe(r.getOdometerKm())).append(", kaina=").append(cost).append(" ").append(nullSafe(r.getCurrency()));

                if (liters != null) {
                    sb.append(", litrai=").append(liters);
                }

                if (r.getDescription() != null && !r.getDescription().isBlank()) {
                    sb.append(", pastabos=").append(r.getDescription());
                }
                sb.append("\n");
                recordLimit++;
            }
        }

        sb.append("\nDokumentai (naujausi pirmi, iki 20):\n");
        int docLimit = 0;
        var docs = documentRepository.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId);
        for (var d : docs) {
            docTypeCounts.put(d.getType(), docTypeCounts.getOrDefault(d.getType(), 0) + 1);

            BigDecimal amount = BigDecimal.ZERO;
            try {
                JsonNode meta = objectMapper.readTree(d.getMetaJson() == null ? "{}" : d.getMetaJson());
                if (meta.hasNonNull("price")) {
                    amount = meta.get("price").decimalValue();
                } else if (meta.hasNonNull("amount")) {
                    amount = meta.get("amount").decimalValue();
                }
            } catch (Exception ignored) {}

            totalDocumentCost = totalDocumentCost.add(amount);

            if (docLimit < 20) {
                sb.append("- [").append(d.getType()).append("] pavadinimas=").append(nullSafe(d.getTitle())).append(", nuo=").append(nullSafe(d.getIssueDate())).append(", iki=").append(nullSafe(d.getExpiresAt())).append(", suma=").append(amount);

                if (d.getDescription() != null && !d.getDescription().isBlank()) {
                    sb.append(", pastabos=").append(d.getDescription());
                }
                sb.append("\n");
                docLimit++;
            }
        }

        sb.append("\nSuvestinė:\n");
        sb.append("- Bendra įrašų suma: ").append(totalRecordCost).append("\n");
        sb.append("- Bendra dokumentų suma: ").append(totalDocumentCost).append("\n");
        sb.append("- Bendra degalų suma: ").append(totalFuelCost).append("\n");
        sb.append("- Bendra degalų litrų suma: ").append(totalFuelLiters).append("\n");
        sb.append("- Įrašų tipų kiekiai: ").append(recordTypeCounts).append("\n");
        sb.append("- Dokumentų tipų kiekiai: ").append(docTypeCounts).append("\n");

        return sb.toString();
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}