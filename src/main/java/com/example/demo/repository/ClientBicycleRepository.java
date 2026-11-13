package com.example.demo.repository;

import com.example.demo.model.client.ClientBicycle;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientBicycleRepository extends JpaRepository<ClientBicycle, Long> {
    void deleteAllByClientId(Long clientId);

    @EntityGraph(attributePaths = "brand")
    List<ClientBicycle> findByClientIdInOrderByIdAsc(Collection<Long> clientIds);
}
