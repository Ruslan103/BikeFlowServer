package com.example.demo.dto.trello;

public record TrelloCardResponse(String id, String name, String url, String idBoard, String idList, boolean closed,
                                 String desc, java.util.List<String> idLabels) {
    public TrelloCardResponse(String id, String name, String url, String idBoard, String idList, boolean closed, String desc) {
        this(id, name, url, idBoard, idList, closed, desc, java.util.List.of());
    }
    public TrelloCardResponse { idLabels = idLabels == null ? java.util.List.of() : java.util.List.copyOf(idLabels); }
}
