package com.example.demo.repository;

import com.example.demo.model.client.ContactType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTypeRepository extends JpaRepository<ContactType, Long> {
    Optional<ContactType> findByCodeIgnoreCase(String code);
}
