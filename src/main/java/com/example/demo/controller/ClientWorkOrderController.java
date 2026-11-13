package com.example.demo.controller;

import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.service.workorder.WorkOrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/{clientId}/work-orders")
@RequiredArgsConstructor
@Slf4j
public class ClientWorkOrderController {

    private final WorkOrderService workOrders;

    @GetMapping
    public List<WorkOrderResponse> findByClientId(@PathVariable Long clientId) {
        log.info("Получен запрос на список бланков клиента");
        return workOrders.findByClientId(clientId);
    }
}
