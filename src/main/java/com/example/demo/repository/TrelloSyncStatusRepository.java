package com.example.demo.repository;

import com.example.demo.model.workorder.TrelloSyncStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrelloSyncStatusRepository extends JpaRepository<TrelloSyncStatus, Long> {

    Optional<TrelloSyncStatus> findByCode(String code);
}
