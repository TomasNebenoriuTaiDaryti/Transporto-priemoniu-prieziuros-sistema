package com.example.back.group.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "group_member")
@IdClass(GroupMemberId.class)
public class GroupMember {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role = "MEMBER";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected GroupMember() {}

    public GroupMember(Long groupId, Long userId, String role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
    }

    public Long getGroupId() { return groupId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
