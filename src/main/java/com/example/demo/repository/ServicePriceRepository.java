package com.example.demo.repository;

import com.example.demo.model.service.ServicePrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    Optional<ServicePrice> findFirstByServiceIdOrderByValidFromDescIdDesc(Long serviceId);
    List<ServicePrice> findByServiceIdOrderByValidFromDescIdDesc(Long serviceId);
    Optional<ServicePrice> findByIdAndServiceId(Long id, Long serviceId);
    void deleteAllByServiceId(Long serviceId);

    @Query("""
            select price from ServicePrice price
            where price.service.id = :serviceId
                and price.validFrom <= :date
                and (price.validTo is null or price.validTo >= :date)
            order by price.validFrom desc
            """)
    List<ServicePrice> findActivePrices(
            @Param("serviceId") Long serviceId,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    @Query("""
            select price from ServicePrice price
            where price.service.id in :serviceIds
                and price.validFrom <= :date
                and (price.validTo is null or price.validTo >= :date)
            order by price.service.id, price.validFrom desc, price.id desc
            """)
    List<ServicePrice> findActivePricesForServices(
            @Param("serviceIds") Collection<Long> serviceIds,
            @Param("date") LocalDate date
    );
}
