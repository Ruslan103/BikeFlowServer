package com.example.demo.service.workorder;

import com.example.demo.dto.WorkOrderRequest;
import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.dto.product.WorkOrderCustomerPartRequest;
import com.example.demo.dto.product.WorkOrderCustomerPartResponse;
import com.example.demo.dto.product.WorkOrderProductItemRequest;
import com.example.demo.dto.product.WorkOrderProductItemResponse;
import java.util.List;

public interface WorkOrderService {

    List<WorkOrderResponse> findAll();

    List<WorkOrderResponse> findByNumber(String blankNumber);

    List<WorkOrderResponse> findByClientId(Long clientId);

    WorkOrderResponse create(WorkOrderRequest request);

    WorkOrderResponse update(String blankNumber, WorkOrderRequest request);

    void delete(String blankNumber);

    List<WorkOrderProductItemResponse> findProducts(String blankNumber);
    WorkOrderProductItemResponse findProduct(String blankNumber, Long itemId);
    WorkOrderProductItemResponse addProduct(String blankNumber, WorkOrderProductItemRequest request);
    WorkOrderProductItemResponse updateProduct(String blankNumber, Long itemId, WorkOrderProductItemRequest request);
    void deleteProduct(String blankNumber, Long itemId);

    List<WorkOrderCustomerPartResponse> findCustomerParts(String blankNumber);
    WorkOrderCustomerPartResponse findCustomerPart(String blankNumber, Long partId);
    WorkOrderCustomerPartResponse addCustomerPart(String blankNumber, WorkOrderCustomerPartRequest request);
    WorkOrderCustomerPartResponse updateCustomerPart(String blankNumber, Long partId, WorkOrderCustomerPartRequest request);
    void deleteCustomerPart(String blankNumber, Long partId);
}
