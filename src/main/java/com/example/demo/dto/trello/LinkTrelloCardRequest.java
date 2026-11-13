package com.example.demo.dto.trello;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkTrelloCardRequest(@NotBlank @Pattern(regexp = "[a-fA-F0-9]{24}|[A-Za-z0-9]{8}") String cardId) { }
