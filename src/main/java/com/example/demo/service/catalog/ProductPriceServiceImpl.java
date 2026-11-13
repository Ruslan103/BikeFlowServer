package com.example.demo.service.catalog;

import com.example.demo.dto.product.CreateProductPriceRequest;
import com.example.demo.dto.product.ProductPriceResponse;
import com.example.demo.dto.product.UpdateProductPriceRequest;
import com.example.demo.model.product.Product;
import com.example.demo.model.product.ProductPrice;
import com.example.demo.repository.ProductPriceRepository;
import com.example.demo.repository.ProductRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPriceServiceImpl implements ProductPriceService {
    private final ProductRepository products;
    private final ProductPriceRepository prices;

    @Override
    @Transactional
    public ProductPriceResponse create(Long productId, CreateProductPriceRequest request) {
        Product product = products.findByIdForPriceUpdate(productId).orElseThrow(() -> productNotFound(productId));
        LocalDate start = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        prices.findFirstByProductIdOrderByValidFromDescIdDesc(productId).ifPresent(previous -> {
            if (!start.isAfter(previous.getValidFrom())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Дата начала цены должна быть позже предыдущей: " + previous.getValidFrom());
            }
            LocalDate end = start.minusDays(1);
            if (previous.getValidTo() == null || previous.getValidTo().isAfter(end)) previous.setValidTo(end);
        });
        ProductPrice saved = prices.save(new ProductPrice(product, request.price(), start));
        return toResponse(saved);
    }

    @Override
    public List<ProductPriceResponse> findAll(Long productId) {
        requireProduct(productId);
        return prices.findByProductIdOrderByValidFromDescIdDesc(productId).stream().map(this::toResponse).toList();
    }

    @Override
    public ProductPriceResponse findById(Long productId, Long priceId) {
        return prices.findByIdAndProductId(priceId, productId).map(this::toResponse)
                .orElseThrow(() -> priceNotFound(priceId));
    }

    @Override
    @Transactional
    public ProductPriceResponse update(Long productId, Long priceId, UpdateProductPriceRequest request) {
        requireProduct(productId);
        ProductPrice price = prices.findByIdAndProductId(priceId, productId)
                .orElseThrow(() -> priceNotFound(priceId));
        price.setPrice(request.price());
        return toResponse(prices.save(price));
    }

    @Override
    @Transactional
    public void delete(Long productId, Long priceId) {
        products.findByIdForPriceUpdate(productId).orElseThrow(() -> productNotFound(productId));
        List<ProductPrice> productPrices = prices.findByProductIdOrderByValidFromDescIdDesc(productId);
        int deletedIndex = -1;
        for (int index = 0; index < productPrices.size(); index++) {
            if (productPrices.get(index).getId().equals(priceId)) {
                deletedIndex = index;
                break;
            }
        }
        if (deletedIndex < 0) throw priceNotFound(priceId);

        ProductPrice previous = deletedIndex + 1 < productPrices.size() ? productPrices.get(deletedIndex + 1) : null;
        ProductPrice next = deletedIndex > 0 ? productPrices.get(deletedIndex - 1) : null;
        if (previous != null) {
            previous.setValidTo(next == null ? null : next.getValidFrom().minusDays(1));
            prices.save(previous);
        }
        prices.delete(productPrices.get(deletedIndex));
        prices.flush();
    }

    private Product requireProduct(Long id) {
        return products.findById(id).orElseThrow(() -> productNotFound(id));
    }

    private ProductPriceResponse toResponse(ProductPrice price) {
        return new ProductPriceResponse(price.getId(), price.getProduct().getId(), price.getPrice(),
                price.getValidFrom(), price.getValidTo());
    }

    private ResponseStatusException productNotFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден: " + id);
    }

    private ResponseStatusException priceNotFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Цена товара не найдена: " + id);
    }
}
