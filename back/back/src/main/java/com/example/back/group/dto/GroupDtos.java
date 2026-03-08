package com.example.back.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class GroupDtos {
    private GroupDtos() {}
    public record CreateGroupRequest(@NotBlank String name) {}
    public record UpdateGroupRequest(@NotBlank String name) {}
    public record InviteRequest(@Email @NotBlank String email) {}
    public record GroupResponse(Long id, String name, String myRole, Long ownerUserId) {}
    public record MemberResponse(Long userId, String email, String role) {}
    public record SimpleMessage(String message) {}
}
