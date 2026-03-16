package com.example.back.record.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "service_log")
public class ServiceRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="vehicle_id", nullable=false)
    private Long vehicleId;

    @Column(name="created_by_user_id")
    private Long createdByUserId;

    @Column(nullable=false, length=20)
    private String type;

    @Column(nullable=false, length=160)
    private String title;

    @Column(columnDefinition="text")
    private String description;

    @Column(name="performed_at", nullable=false)
    private Instant performedAt;

    @Column(name="odometer_km")
    private Long odometerKm;

    @Column(name="labor_cost", nullable=false, precision = 12, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Column(name="parts_cost", nullable=false, precision = 12, scale = 2)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name="total_cost", nullable=false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable=false, length = 3)
    private String currency = "EUR";

    @Column(nullable=false, length=30)
    private String kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="meta", columnDefinition="jsonb", nullable=false)
    private String metaJson = "{}";

    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();

    public ServiceRecord() {}

    public Long getId() { return id; }
    public Long getVehicleId() { return vehicleId; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getPerformedAt() { return performedAt; }
    public Long getOdometerKm() { return odometerKm; }
    public String getKind() { return kind; }
    public String getMetaJson() { return metaJson; }
    public BigDecimal getTotalCost() { return totalCost; }
    public String getCurrency() { return currency; }

    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }
    public void setOdometerKm(Long odometerKm) { this.odometerKm = odometerKm; }
    public void setKind(String kind) { this.kind = kind; }
    public void setMetaJson(String metaJson) { this.metaJson = metaJson; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setTotalCost(BigDecimal bigDecimal) { this.totalCost = bigDecimal; }
}
