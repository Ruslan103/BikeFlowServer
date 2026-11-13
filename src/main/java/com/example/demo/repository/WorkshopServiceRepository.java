package com.example.demo.repository;

import com.example.demo.model.service.WorkshopService;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkshopServiceRepository extends JpaRepository<WorkshopService, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select service from WorkshopService service where service.id = :id")
    Optional<WorkshopService> findByIdForPriceUpdate(@Param("id") Long id);

    @Query(value = """
            select service.*
            from services service
            where position(lower(:query) in lower(service.name)) > 0
               or lower(:query) <% lower(service.name)
               or lower(service.name) % lower(:query)
            order by
                case
                    when lower(service.name) = lower(:query) then 0
                    when left(lower(service.name), length(:query)) = lower(:query) then 1
                    when position(lower(:query) in lower(service.name)) > 0 then 2
                    else 3
                end,
                word_similarity(lower(:query), lower(service.name)) desc,
                similarity(lower(service.name), lower(:query)) desc,
                service.id
            """, nativeQuery = true)
    List<WorkshopService> searchByName(@Param("query") String query, Pageable pageable);
}
