package com.example.demo.controller;

import com.example.demo.dto.client.ContactTypeResponse;
import com.example.demo.service.client.ContactTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact-types")
@RequiredArgsConstructor
public class ContactTypeController {
    private final ContactTypeService types;
    @GetMapping public List<ContactTypeResponse> findAll() { return types.findAll(); }
}
