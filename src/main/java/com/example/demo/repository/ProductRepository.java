package com.example.demo.repository;

import com.example.demo.model.product.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForPriceUpdate(@Param("id") Long id);

    @Query(value = """
            select product.*
            from products product
            where position(lower(:query) in lower(product.name)) > 0
               or lower(:query) <% lower(product.name)
               or lower(product.name) % lower(:query)
            order by
                case
                    when lower(product.name) = lower(:query) then 0
                    when left(lower(product.name), length(:query)) = lower(:query) then 1
                    when position(lower(:query) in lower(product.name)) > 0 then 2
                    else 3
                end,
                word_similarity(lower(:query), lower(product.name)) desc,
                similarity(lower(product.name), lower(:query)) desc,
                product.id
            """, nativeQuery = true)
    List<Product> searchByName(@Param("query") String query, Pageable pageable);
}
