package com.example.demo.service.workorder;

import com.example.demo.dto.workorder.ServiceQuoteRequest;
import com.example.demo.dto.workorder.ServiceQuoteResponse;

public interface ServiceQuoteService {
    ServiceQuoteResponse calculate(ServiceQuoteRequest request);
}
