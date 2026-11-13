package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrderStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderStatusRepository extends JpaRepository<WorkOrderStatus, Long> {

    Optional<WorkOrderStatus> findByCode(String code);
}
