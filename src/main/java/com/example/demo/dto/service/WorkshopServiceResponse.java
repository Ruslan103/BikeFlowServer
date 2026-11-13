package com.example.demo.dto.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkshopServiceResponse(Long id, String name, String description,
                                      BigDecimal price, LocalDate validFrom, LocalDate validTo) { }
