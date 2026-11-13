package com.example.demo.service.discount;

import com.example.demo.dto.discount.DiscountRequest;
import com.example.demo.dto.discount.DiscountResponse;
import com.example.demo.dto.discount.DiscountBatchResponse;
import java.util.List;

public interface DiscountService {
    DiscountResponse create(DiscountRequest request);
    List<DiscountResponse> findAll();
    DiscountBatchResponse findBatch(int page, int size);
    List<DiscountResponse> search(String query);
    DiscountResponse findById(Long id);
    DiscountResponse update(Long id, DiscountRequest request);
    void delete(Long id);
}
