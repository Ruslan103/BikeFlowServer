package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrderProductItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderProductItemRepository extends JpaRepository<WorkOrderProductItem, Long> {
    List<WorkOrderProductItem> findAllByWorkOrderIdOrderByIdAsc(Long workOrderId);
    Optional<WorkOrderProductItem> findByIdAndWorkOrderId(Long id, Long workOrderId);
    void deleteAllByWorkOrderId(Long workOrderId);
    boolean existsByProductId(Long productId);
}
