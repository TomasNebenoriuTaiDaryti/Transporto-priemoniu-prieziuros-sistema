package com.example.back.vehicle.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(length = 17)
    private String vin;

    private String make;
    private String model;

    @Column(name = "model_year")
    private Integer modelYear;

    private String transmission;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "odometer_km", nullable = false)
    private Long odometerKm = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "engine_displacement_cc")
    private Integer engineDisplacementCc;

    @Column(name = "drive", length = 60)
    private String drive;

    @Column(name = "body", length = 120)
    private String body;

    @Column(name = "doors")
    private Integer doors;

    @Column(name = "seats")
    private Integer seats;

    @Column(name = "co2_g_km", precision = 10, scale = 2)
    private BigDecimal co2Gkm;

    @Column(name = "plant_country", length = 80)
    private String plantCountry;

    @Column(name = "manufacturer", length = 200)
    private String manufacturer;

    public Vehicle() {}

    public Vehicle(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
        this.odometerKm = 0L;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public String getVin() { return vin; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public Integer getModelYear() { return modelYear; }
    public String getTransmission() { return transmission; }
    public String getFuelType() { return fuelType; }
    public Long getOdometerKm() { return odometerKm; }
    public Instant getCreatedAt() { return createdAt; }
    public Integer getEngineDisplacementCc() { return engineDisplacementCc; }
    public String getDrive() { return drive; }
    public String getBody() { return body; }
    public Integer getDoors() { return doors; }
    public Integer getSeats() { return seats; }
    public BigDecimal getCo2Gkm() { return co2Gkm; }
    public String getPlantCountry() { return plantCountry; }
    public String getManufacturer() { return manufacturer; }

    public void setId(Long vehicleId) { this.id = vehicleId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setVin(String vin) { this.vin = vin; }
    public void setMake(String make) { this.make = normalizeBlankToNull(make); }
    public void setModel(String model) { this.model = normalizeBlankToNull(model); }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public void setTransmission(String transmission) { this.transmission = normalizeBlankToNull(transmission); }
    public void setFuelType(String fuelType) { this.fuelType = normalizeBlankToNull(fuelType); }

    public void setOdometerKm(Long odometerKm) {
        if (odometerKm == null) return;
        if (odometerKm < 0) throw new IllegalArgumentException("Odometer km negali būti neigiamas");
        this.odometerKm = odometerKm;
    }

    public void setEngineDisplacementCc(Integer engineDisplacementCc) {
        if (engineDisplacementCc == null) {
            this.engineDisplacementCc = null;
            return;
        }
        if (engineDisplacementCc <= 300 || engineDisplacementCc > 10000) {
            throw new IllegalArgumentException("Engine displacement cc neteisingas");
        }
        this.engineDisplacementCc = engineDisplacementCc;
    }

    public void setDrive(String drive) { this.drive = normalizeBlankToNull(drive); }
    public void setBody(String body) { this.body = normalizeBlankToNull(body); }

    public void setDoors(Integer doors) {
        if (doors == null) { this.doors = null; return; }
        if (doors < 1 || doors > 8) throw new IllegalArgumentException("Doors neteisingas");
        this.doors = doors;
    }

    public void setSeats(Integer seats) {
        if (seats == null) { this.seats = null; return; }
        if (seats < 1 || seats > 20) throw new IllegalArgumentException("Seats neteisingas");
        this.seats = seats;
    }

    public void setCo2Gkm(BigDecimal co2Gkm) { this.co2Gkm = co2Gkm; }
    public void setPlantCountry(String plantCountry) { this.plantCountry = normalizeBlankToNull(plantCountry); }
    public void setManufacturer(String manufacturer) { this.manufacturer = normalizeBlankToNull(manufacturer); }

    private static String normalizeBlankToNull(String s) {
        if (s == null) return null;
        String x = s.trim();
        return x.isEmpty() ? null : x;
    }

}