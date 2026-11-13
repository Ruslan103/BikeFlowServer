package com.example.demo.service.workorder;

import com.example.demo.dto.WorkOrderServiceItemResponse;
import com.example.demo.dto.workorder.ServiceQuoteLineRequest;
import com.example.demo.dto.workorder.ServiceQuoteRequest;
import com.example.demo.dto.workorder.ServiceQuoteResponse;
import com.example.demo.model.service.Discount;
import com.example.demo.repository.DiscountRepository;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceQuoteServiceImpl implements ServiceQuoteService {
    private final WorkshopServiceRepository services;
    private final ServicePriceRepository prices;
    private final DiscountRepository discounts;

    @Override
    public ServiceQuoteResponse calculate(ServiceQuoteRequest request) {
        LocalDate today = LocalDate.now();
        var lines = request.services().stream().map(line -> calculateLine(line, today)).toList();
        BigDecimal total = lines.stream().map(WorkOrderServiceItemResponse::priceWithoutDiscount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal discounted = lines.stream().map(WorkOrderServiceItemResponse::priceWithDiscount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new ServiceQuoteResponse(lines, lines.size(), total, discounted);
    }

    private WorkOrderServiceItemResponse calculateLine(ServiceQuoteLineRequest line, LocalDate today) {
        var service = services.findById(line.serviceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + line.serviceId()));
        BigDecimal price = line.priceWithoutDiscount();
        if (price == null) {
            price = prices.findActivePrices(line.serviceId(), today, PageRequest.of(0, 1)).stream()
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No active price configured for service: " + line.serviceId())).getPrice();
        }
        Discount discount = line.discountId() == null ? null : discounts.findById(line.discountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Discount not found: " + line.discountId()));
        BigDecimal finalPrice = line.priceWithDiscount();
        if (finalPrice == null) {
            BigDecimal percentage = discount == null ? BigDecimal.ZERO : discount.getPercentage();
            finalPrice = price.multiply(BigDecimal.valueOf(100).subtract(percentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return new WorkOrderServiceItemResponse(service.getId(),
                line.serviceNameSnapshot() == null ? service.getName() : line.serviceNameSnapshot(),
                discount == null ? null : discount.getId(), discount == null ? null : discount.getName(),
                discount == null ? null : discount.getPercentage(), price.setScale(2, RoundingMode.UNNECESSARY),
                finalPrice.setScale(2, RoundingMode.UNNECESSARY));
    }
}
