package com.example.demo.dto.client;

import java.util.List;

public record ClientSearchResponse(Long id, Long publicId, String lastName, String firstName,
                                   String middleName, String comment,
                                   List<ClientBicycleResponse> bicycles, List<ClientContactResponse> contacts,
                                   com.example.demo.model.client.ClientType type, String organizationName,
                                   String representative, Long version) {
    public ClientSearchResponse(Long id, Long publicId, String lastName, String firstName, String middleName,
            String comment, List<ClientBicycleResponse> bicycles, List<ClientContactResponse> contacts) {
        this(id, publicId, lastName, firstName, middleName, comment, bicycles, contacts,
                com.example.demo.model.client.ClientType.PERSON, null, null, 0L);
    }
}
