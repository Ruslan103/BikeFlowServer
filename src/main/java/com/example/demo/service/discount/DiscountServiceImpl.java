package com.example.demo.service.discount;

import com.example.demo.dto.discount.DiscountRequest;
import com.example.demo.dto.discount.DiscountResponse;
import com.example.demo.dto.discount.DiscountBatchResponse;
import com.example.demo.model.service.Discount;
import com.example.demo.repository.DiscountRepository;
import java.util.List;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DiscountServiceImpl implements DiscountService {
    private final DiscountRepository discounts;

    @Override
    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        return toResponse(discounts.save(new Discount(request.name().strip(), request.percentage())));
    }

    @Override
    public List<DiscountResponse> findAll() {
        return discounts.findAll(Sort.by("name").and(Sort.by("id"))).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public DiscountBatchResponse findBatch(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Номер страницы должен быть неотрицательным, размер пачки — от 1 до 100.");
        }
        Page<DiscountResponse> result = discounts.findAll(PageRequest.of(page, size,
                        Sort.by("name").and(Sort.by("id"))))
                .map(this::toResponse);
        return new DiscountBatchResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.hasNext(), result.hasPrevious());
    }

    @Override
    public List<DiscountResponse> search(String query) {
        String normalizedQuery = query == null ? "" : query.strip();
        if (normalizedQuery.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название или процент скидки.");
        }
        if (normalizedQuery.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Запрос слишком длинный.");
        }

        log.info("Начат поиск скидок");
        BigDecimal percentage = parsePercentage(normalizedQuery);
        return discounts.searchByNameOrPercentage(normalizedQuery, percentage, PageRequest.of(0, 50)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DiscountResponse findById(Long id) {
        return toResponse(requireDiscount(id));
    }

    @Override
    @Transactional
    public DiscountResponse update(Long id, DiscountRequest request) {
        Discount discount = requireDiscount(id);
        discount.setName(request.name().strip());
        discount.setPercentage(request.percentage());
        return toResponse(discount);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        discounts.delete(requireDiscount(id));
        // Проверяем ограничения БД до завершения операции: связанные скидки удалять нельзя.
        discounts.flush();
    }

    private Discount requireDiscount(Long id) {
        return discounts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found: " + id));
    }

    private DiscountResponse toResponse(Discount discount) {
        return new DiscountResponse(discount.getId(), discount.getName(), discount.getPercentage());
    }

    private BigDecimal parsePercentage(String query) {
        String value = query.endsWith("%") ? query.substring(0, query.length() - 1).strip() : query;
        if (!value.matches("[0-9]{1,3}([.,][0-9]{1,2})?")) return null;
        BigDecimal percentage = new BigDecimal(value.replace(',', '.'));
        return percentage.compareTo(BigDecimal.ZERO) >= 0 && percentage.compareTo(BigDecimal.valueOf(100)) <= 0
                ? percentage : null;
    }
}
