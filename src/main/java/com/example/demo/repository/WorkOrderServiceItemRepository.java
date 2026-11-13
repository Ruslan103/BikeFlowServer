package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrderServiceItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderServiceItemRepository extends JpaRepository<WorkOrderServiceItem, Long> {

    List<WorkOrderServiceItem> findAllByWorkOrderIdOrderByIdAsc(Long workOrderId);

    boolean existsByServiceId(Long serviceId);

    void deleteAllByWorkOrderId(Long workOrderId);
}
