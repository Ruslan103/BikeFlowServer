package com.example.demo.repository;

import com.example.demo.model.client.BicycleBrand;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BicycleBrandRepository extends JpaRepository<BicycleBrand, Long> {
    Optional<BicycleBrand> findByNameIgnoreCase(String name);
}
