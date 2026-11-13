package com.example.demo.repository;

import com.example.demo.model.service.Discount;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    @Query(value = """
            select discount.*
            from discounts discount
            where position(lower(:query) in lower(discount.name)) > 0
               or (length(:query) >= 4 and (
                    lower(:query) <% lower(discount.name)
                    or lower(discount.name) % lower(:query)
               ))
               or (cast(:percentage as numeric) is not null
                    and (discount.percentage = cast(:percentage as numeric)
                         or cast(discount.percentage as text) like '%' || cast(:percentage as text) || '%'))
            order by
                case
                    when lower(discount.name) = lower(:query) then 0
                    when cast(:percentage as numeric) is not null
                         and discount.percentage = cast(:percentage as numeric) then 1
                    when cast(:percentage as numeric) is not null
                         and cast(discount.percentage as text) like '%' || cast(:percentage as text) || '%' then 2
                    when left(lower(discount.name), length(:query)) = lower(:query) then 3
                    when position(lower(:query) in lower(discount.name)) > 0 then 4
                    else 5
                end,
                case when cast(:percentage as numeric) is null then 0
                     else abs(discount.percentage - cast(:percentage as numeric)) end,
                word_similarity(lower(:query), lower(discount.name)) desc,
                similarity(lower(discount.name), lower(:query)) desc,
                discount.id
            """, nativeQuery = true)
    List<Discount> searchByNameOrPercentage(
            @Param("query") String query,
            @Param("percentage") BigDecimal percentage,
            Pageable pageable
    );
}
