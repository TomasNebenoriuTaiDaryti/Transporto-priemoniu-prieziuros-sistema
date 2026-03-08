package com.example.back.group.repo;

import com.example.back.group.domain.GroupMember;
import com.example.back.group.domain.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepo extends JpaRepository<GroupMember, GroupMemberId> {

    List<GroupMember> findAllByUserId(Long userId);
    List<GroupMember> findAllByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}
