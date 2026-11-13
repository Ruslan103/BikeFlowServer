package com.example.demo.dto.client;

import java.util.List;

public record ClientSearchResult(List<ClientSearchResponse> exact, List<ClientSearchResponse> similar, boolean autoSelect) {
    public ClientSearchResult(List<ClientSearchResponse> exact,List<ClientSearchResponse> similar){this(exact,similar,false);}
}
