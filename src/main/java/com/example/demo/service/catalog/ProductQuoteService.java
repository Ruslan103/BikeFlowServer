package com.example.demo.service.catalog;

import com.example.demo.dto.product.ProductQuoteRequest;
import com.example.demo.dto.product.ProductQuoteResponse;

public interface ProductQuoteService {
    ProductQuoteResponse calculate(ProductQuoteRequest request);
}
