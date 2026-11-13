package com.example.demo.service.catalog;

import com.example.demo.dto.product.CreateProductPriceRequest;
import com.example.demo.dto.product.ProductPriceResponse;
import com.example.demo.dto.product.UpdateProductPriceRequest;
import java.util.List;

public interface ProductPriceService {
    ProductPriceResponse create(Long productId, CreateProductPriceRequest request);
    List<ProductPriceResponse> findAll(Long productId);
    ProductPriceResponse findById(Long productId, Long priceId);
    ProductPriceResponse update(Long productId, Long priceId, UpdateProductPriceRequest request);
    void delete(Long productId, Long priceId);
}
