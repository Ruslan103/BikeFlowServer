package com.example.demo.dto.workorder;

import com.example.demo.dto.WorkOrderServiceItemResponse;
import java.math.BigDecimal;
import java.util.List;

public record ServiceQuoteResponse(List<WorkOrderServiceItemResponse> services, int serviceCount,
                                   BigDecimal totalPrice, BigDecimal totalWithDiscount) { }
