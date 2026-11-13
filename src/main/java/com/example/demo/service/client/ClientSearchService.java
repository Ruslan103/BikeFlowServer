package com.example.demo.service.client;

import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.dto.client.ClientBatchResponse;
import java.util.List;

public interface ClientSearchService {
    com.example.demo.dto.client.ClientSearchResult search(String query);
    ClientBatchResponse findBatch(int page, int size);
    ClientSearchResponse findById(Long id);
    List<ClientSearchResponse> findByPhone(String phone);
}
