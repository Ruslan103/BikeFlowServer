package com.example.demo.dto.client;

public record ClientContactResponse(Long id, Long typeId, String typeCode, String typeName,
                                     String value, String label) { }
