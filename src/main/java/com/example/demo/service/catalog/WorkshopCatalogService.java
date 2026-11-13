package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateWorkshopServiceRequest;
import com.example.demo.dto.service.WorkshopServiceResponse;
import com.example.demo.dto.service.WorkshopServiceBatchResponse;
import com.example.demo.dto.service.UpdateWorkshopServiceRequest;
import java.util.List;

public interface WorkshopCatalogService {
    WorkshopServiceResponse create(CreateWorkshopServiceRequest request);
    WorkshopServiceResponse update(Long id, UpdateWorkshopServiceRequest request);
    void delete(Long id);
    WorkshopServiceResponse findById(Long id);
    List<WorkshopServiceResponse> findAll();
    WorkshopServiceBatchResponse findBatch(int page, int size);
    List<WorkshopServiceResponse> searchByName(String query);
}
