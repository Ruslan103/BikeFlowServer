package com.example.demo.dto.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServicePriceResponse(Long id, Long serviceId, BigDecimal price,
                                   LocalDate validFrom, LocalDate validTo) { }
