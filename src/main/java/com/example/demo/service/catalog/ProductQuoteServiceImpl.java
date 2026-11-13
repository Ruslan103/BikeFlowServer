package com.example.demo.service.catalog;

import com.example.demo.dto.product.ProductQuoteLineRequest;
import com.example.demo.dto.product.ProductQuoteRequest;
import com.example.demo.dto.product.ProductQuoteResponse;
import com.example.demo.dto.product.WorkOrderProductItemResponse;
import com.example.demo.model.product.Product;
import com.example.demo.model.product.ProductPrice;
import com.example.demo.model.service.Discount;
import com.example.demo.repository.DiscountRepository;
import com.example.demo.repository.ProductPriceRepository;
import com.example.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQuoteServiceImpl implements ProductQuoteService {
    private final ProductRepository products;
    private final ProductPriceRepository prices;
    private final DiscountRepository discounts;

    @Override
    public ProductQuoteResponse calculate(ProductQuoteRequest request) {
        LocalDate today = LocalDate.now();
        List<WorkOrderProductItemResponse> lines = request.products().stream()
                .map(line -> calculateLine(line, today)).toList();
        BigDecimal total = lines.stream().map(line -> line.priceWithoutDiscount().multiply(line.quantity())
                        .setScale(2, RoundingMode.HALF_UP))
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal discounted = lines.stream().map(WorkOrderProductItemResponse::lineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new ProductQuoteResponse(lines, total, discounted);
    }

    private WorkOrderProductItemResponse calculateLine(ProductQuoteLineRequest request, LocalDate today) {
        Product product = products.findById(request.productId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден: " + request.productId()));
        if (!product.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Товар отключён.");
        ProductPrice price = prices.findActivePrices(product.getId(), today, PageRequest.of(0, 1)).stream()
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Для товара нет действующей цены: " + product.getId()));
        Discount discount = request.discountId() == null ? null : discounts.findById(request.discountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Скидка не найдена: " + request.discountId()));
        BigDecimal basePrice = price.getPrice();
        BigDecimal percentage = discount == null ? BigDecimal.ZERO : discount.getPercentage();
        BigDecimal finalPrice = basePrice.multiply(BigDecimal.valueOf(100).subtract(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new WorkOrderProductItemResponse(null, product.getId(), product.getName(), product.getArticle(),
                product.getUnit(), request.quantity(), discount == null ? null : discount.getId(),
                discount == null ? null : discount.getName(), discount == null ? null : discount.getPercentage(),
                basePrice, finalPrice, finalPrice.multiply(request.quantity()).setScale(2, RoundingMode.HALF_UP),
                com.example.demo.model.workorder.ProductSupplyStatus.IN_STOCK);
    }
}
