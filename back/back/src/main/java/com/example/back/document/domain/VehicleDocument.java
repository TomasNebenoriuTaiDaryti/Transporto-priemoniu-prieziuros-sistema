package com.example.back.document.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "document")
public class VehicleDocument {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 600)
    private String description;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
    private String metaJson = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public VehicleDocument() {}

    public UUID getId() { return id; }
    public Long getVehicleId() { return vehicleId; }
    public Long getUploadedByUserId() { return uploadedByUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public String getMetaJson() { return metaJson; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public void setUploadedByUserId(Long uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
    public void setMetaJson(String metaJson) { this.metaJson = metaJson; }

}