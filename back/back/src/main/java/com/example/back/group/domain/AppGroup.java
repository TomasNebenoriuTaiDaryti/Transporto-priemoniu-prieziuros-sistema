package com.example.back.group.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_group")
public class AppGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AppGroup() {}

    public AppGroup(String name, Long ownerUserId) {
        this.name = name;
        this.ownerUserId = ownerUserId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setName(String name) { this.name = name; }
}