package com.example.demo.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductQuoteResponse(List<WorkOrderProductItemResponse> products,
                                   BigDecimal totalPrice, BigDecimal totalWithDiscount) { }
