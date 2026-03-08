package com.example.back.group.repo;

import com.example.back.group.domain.AppGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppGroupRepo extends JpaRepository<AppGroup, Long> {
    List<AppGroup> findAllByOwnerUserId(Long ownerUserId);
}