package com.example.demo.service.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ClientCardConflict extends ResponseStatusException {
    private final String code;
    private final String searchQuery;
    public ClientCardConflict(String code, String message) { this(code, message, null); }
    public ClientCardConflict(String code, String message, String searchQuery) {
        super(HttpStatus.CONFLICT, message);
        this.code = code;
        this.searchQuery = searchQuery;
    }
    public String code() { return code; }
    public String searchQuery() { return searchQuery; }
}
