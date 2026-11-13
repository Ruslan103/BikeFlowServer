package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrderCustomerPart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderCustomerPartRepository extends JpaRepository<WorkOrderCustomerPart, Long> {
    List<WorkOrderCustomerPart> findAllByWorkOrderIdOrderByIdAsc(Long workOrderId);
    Optional<WorkOrderCustomerPart> findByIdAndWorkOrderId(Long id, Long workOrderId);
    long countByWorkOrderId(Long workOrderId);
    void deleteAllByWorkOrderId(Long workOrderId);
}
