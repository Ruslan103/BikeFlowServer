package com.example.demo.repository;

import com.example.demo.model.product.ProductPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {
    Optional<ProductPrice> findFirstByProductIdOrderByValidFromDescIdDesc(Long productId);
    List<ProductPrice> findByProductIdOrderByValidFromDescIdDesc(Long productId);
    Optional<ProductPrice> findByIdAndProductId(Long id, Long productId);
    void deleteAllByProductId(Long productId);

    @Query("""
            select price from ProductPrice price
            where price.product.id = :productId
                and price.validFrom <= :date
                and (price.validTo is null or price.validTo >= :date)
            order by price.validFrom desc
            """)
    List<ProductPrice> findActivePrices(
            @Param("productId") Long productId,
            @Param("date") LocalDate date,
            Pageable pageable
    );
}
