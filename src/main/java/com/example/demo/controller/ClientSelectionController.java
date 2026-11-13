package com.example.demo.controller;

import com.example.demo.dto.client.ClientSelectionRequest;
import com.example.demo.dto.client.ClientSelectionResponse;
import com.example.demo.service.workorder.WorkOrderSnapshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClientSelectionController {
    private final WorkOrderSnapshotService snapshots;
    @PostMapping("/api/work-orders/client-selection")
    public ClientSelectionResponse preview(@Valid @RequestBody ClientSelectionRequest request) { return snapshots.preview(request); }
}
