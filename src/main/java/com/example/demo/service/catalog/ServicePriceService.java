package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateServicePriceRequest;
import com.example.demo.dto.service.ServicePriceResponse;
import com.example.demo.dto.service.UpdateServicePriceRequest;
import java.util.List;

public interface ServicePriceService {
    ServicePriceResponse create(Long serviceId, CreateServicePriceRequest request);
    List<ServicePriceResponse> findAll(Long serviceId);
    ServicePriceResponse findById(Long serviceId, Long priceId);
    ServicePriceResponse update(Long serviceId, Long priceId, UpdateServicePriceRequest request);
    void delete(Long serviceId, Long priceId);
}
