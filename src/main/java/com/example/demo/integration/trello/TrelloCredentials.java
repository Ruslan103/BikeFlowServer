package com.example.demo.integration.trello;

public record TrelloCredentials(String apiKey, String token) {
    @Override public String toString() { return "TrelloCredentials[redacted]"; }
}
