package com.example.demo.service.catalog;

import com.example.demo.dto.product.CreateProductRequest;
import com.example.demo.dto.product.ProductResponse;
import com.example.demo.dto.product.UpdateProductRequest;
import com.example.demo.model.product.Product;
import com.example.demo.model.product.ProductPrice;
import com.example.demo.repository.ProductPriceRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.WorkOrderProductItemRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductCatalogServiceImpl implements ProductCatalogService {
    private final ProductRepository products;
    private final ProductPriceRepository prices;
    private final WorkOrderProductItemRepository workOrderProducts;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = products.save(new Product(request.name().strip(), clean(request.article()),
                clean(request.category()), request.unit().strip(), request.description()));
        LocalDate validFrom = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        ProductPrice price = prices.save(new ProductPrice(product, request.price(), validFrom));
        log.info("Создан товар, ID {}", product.getId());
        return toResponse(product, price);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = products.findById(id).orElseThrow(() -> notFound(id));
        product.setName(request.name().strip());
        product.setArticle(clean(request.article()));
        product.setCategory(clean(request.category()));
        product.setUnit(request.unit().strip());
        product.setDescription(request.description());
        product.setActive(request.active());
        Product saved = products.save(product);
        return withCurrentPrice(saved, LocalDate.now());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = products.findByIdForPriceUpdate(id).orElseThrow(() -> notFound(id));
        if (workOrderProducts.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Товар используется в заказах и не может быть удалён. Отключите его вместо удаления.");
        }
        prices.deleteAllByProductId(id);
        products.delete(product);
        products.flush();
    }

    @Override
    public ProductResponse findById(Long id) {
        Product product = products.findById(id).orElseThrow(() -> notFound(id));
        return withCurrentPrice(product, LocalDate.now());
    }

    @Override
    public List<ProductResponse> findAll() {
        LocalDate today = LocalDate.now();
        return products.findAll(Sort.by("name").and(Sort.by("id"))).stream()
                .map(product -> withCurrentPrice(product, today)).toList();
    }

    @Override
    public List<ProductResponse> searchByName(String query) {
        String normalizedQuery = query == null ? "" : query.strip();
        if (normalizedQuery.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название или часть названия товара.");
        }
        if (normalizedQuery.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Запрос слишком длинный.");
        }
        return products.searchByName(normalizedQuery, PageRequest.of(0, 50)).stream()
                .map(product -> withCurrentPrice(product, LocalDate.now())).toList();
    }

    private ProductResponse withCurrentPrice(Product product, LocalDate date) {
        ProductPrice price = prices.findActivePrices(product.getId(), date, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        return toResponse(product, price);
    }

    private ProductResponse toResponse(Product product, ProductPrice price) {
        return new ProductResponse(product.getId(), product.getName(), product.getArticle(), product.getCategory(),
                product.getUnit(), product.getDescription(), product.isActive(),
                price == null ? null : price.getPrice(), price == null ? null : price.getValidFrom(),
                price == null ? null : price.getValidTo());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден: " + id);
    }
}
