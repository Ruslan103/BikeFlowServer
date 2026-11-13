package com.example.demo.controller;

import com.example.demo.dto.auth.RoleResponse;
import com.example.demo.repository.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    private final RoleRepository roles;

    @GetMapping("/api/roles")
    public List<RoleResponse> findAll() {
        log.info("Получен запрос на список ролей");
        return roles.findAll(Sort.by("code")).stream()
                .map(role -> new RoleResponse(role.getCode(), role.getName())).toList();
    }
}
