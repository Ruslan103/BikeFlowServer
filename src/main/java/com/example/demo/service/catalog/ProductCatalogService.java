package com.example.demo.service.catalog;

import com.example.demo.dto.product.CreateProductRequest;
import com.example.demo.dto.product.ProductResponse;
import com.example.demo.dto.product.UpdateProductRequest;
import java.util.List;

public interface ProductCatalogService {
    ProductResponse create(CreateProductRequest request);
    ProductResponse update(Long id, UpdateProductRequest request);
    void delete(Long id);
    ProductResponse findById(Long id);
    List<ProductResponse> findAll();
    List<ProductResponse> searchByName(String query);
}
