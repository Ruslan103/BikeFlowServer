package com.example.demo.service.client;

import com.example.demo.dto.client.ContactTypeResponse;
import com.example.demo.repository.ContactTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactTypeService {
    private final ContactTypeRepository types;

    public List<ContactTypeResponse> findAll() {
        return types.findAll(Sort.by("name").and(Sort.by("id"))).stream()
                .map(type -> new ContactTypeResponse(type.getId(), type.getCode(), type.getName())).toList();
    }
}
