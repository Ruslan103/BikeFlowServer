package com.example.demo.integration.trello;

import com.example.demo.dto.trello.TrelloBoardResponse;
import com.example.demo.dto.trello.TrelloCardResponse;
import com.example.demo.dto.trello.TrelloLabelResponse;
import com.example.demo.dto.trello.TrelloListResponse;
import com.example.demo.dto.trello.TrelloMemberResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpTrelloClient implements TrelloClient {
    private final RestClient http;
    private final TrelloCredentialsProvider credentials;

    @Autowired
    public HttpTrelloClient(TrelloCredentialsProvider credentials) {
        this(buildClient(RestClient.builder()), credentials);
    }

    HttpTrelloClient(RestClient http, TrelloCredentialsProvider credentials) {
        this.http = http;
        this.credentials = credentials;
    }

    private static RestClient buildClient(RestClient.Builder builder) {
        var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofSeconds(20));
        return builder.clone().baseUrl("https://api.trello.com/1").requestFactory(factory).build();
    }

    @Override public TrelloMemberResponse member() {
        return get("/members/me?fields=username,fullName", ParameterizedTypeReference.forType(TrelloMemberResponse.class));
    }
    @Override public List<TrelloBoardResponse> boards() {
        return get("/members/me/boards?filter=open&fields=name,url", new ParameterizedTypeReference<>() { });
    }
    @Override public List<TrelloListResponse> lists(String boardId) {
        return get("/boards/" + boardId + "/lists?filter=open&fields=name,idBoard,closed", new ParameterizedTypeReference<>() { });
    }
    @Override public List<TrelloLabelResponse> labels(String boardId) {
        return get("/boards/" + boardId + "/labels?limit=1000&fields=name,color,idBoard", new ParameterizedTypeReference<>() { });
    }
    @Override public List<TrelloCardResponse> cards(String listId) {
        return get("/lists/" + listId + "/cards?fields=name,url,idBoard,idList,closed,desc,idLabels", new ParameterizedTypeReference<>() { });
    }
    @Override public TrelloCardResponse card(String cardId) {
        return get("/cards/" + cardId + "?fields=name,url,idBoard,idList,closed,desc,idLabels", ParameterizedTypeReference.forType(TrelloCardResponse.class));
    }

    @Override public TrelloListResponse list(String listId) {
        return get("/lists/" + listId + "?fields=name,idBoard,closed", ParameterizedTypeReference.forType(TrelloListResponse.class));
    }
    @Override public List<TrelloCardResponse> boardCards(String boardId) {
        return get("/boards/" + boardId + "/cards?filter=all&fields=name,url,idBoard,idList,closed,desc,idLabels", new ParameterizedTypeReference<>() { });
    }
    @Override public TrelloCardResponse createCard(String listId, String name, String description, List<String> labelIds) {
        return writeCard(http.post().uri("/cards"),
                Map.of("idList", listId, "name", name, "desc", description, "idLabels", labelIds));
    }

    @Override public TrelloCardResponse updateCard(String cardId, String boardId, String listId, String name,
                                                  String description, List<String> labelIds) {
        return writeCard(http.put().uri("/cards/" + cardId), Map.of("idBoard", boardId, "idList", listId,
                "name", name, "desc", description, "idLabels", labelIds));
    }

    private TrelloCardResponse writeCard(RestClient.RequestBodySpec request, Map<String, Object> body) {
        String authorization = authorization();
        try {
            return read(request.header("Authorization", authorization)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body),
                    ParameterizedTypeReference.forType(TrelloCardResponse.class));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Не удалось подтвердить изменение карточки Trello. При повторе сервер проверит привязку.");
        }
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type) {
        String authorization = authorization();
        try {
            return read(http.get().uri(path).header("Authorization", authorization), type);
        } catch (RestClientException exception) {
            // Не включаем исходное исключение: оно может содержать данные внешнего запроса.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Не удалось получить ответ Trello. Повторите запрос позже.");
        }
    }

    private <T> T read(RestClient.RequestHeadersSpec<?> request, ParameterizedTypeReference<T> type) {
        T body = request.retrieve().onStatus(status -> !status.is2xxSuccessful(), (sent, response) -> {
            throw upstreamError(response.getStatusCode());
        }).body(type);
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Trello вернул пустой ответ.");
        return body;
    }

    private String authorization() {
        TrelloCredentials value = credentials.trelloCredentials();
        if (value == null || value.apiKey() == null || value.token() == null
                || !value.apiKey().matches("[A-Za-z0-9_-]+") || !value.token().matches("[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Укажите API-ключ и токен Trello в личном кабинете сервера.");
        }
        return "OAuth oauth_consumer_key=\"" + value.apiKey() + "\", oauth_token=\"" + value.token() + "\"";
    }

    private ResponseStatusException upstreamError(HttpStatusCode status) {
        return switch (status.value()) {
            case 401, 403 -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Trello отклонил доступ. Проверьте API-ключ, токен и права на доску.");
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Объект Trello не найден или недоступен.");
            case 429 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Превышен лимит запросов Trello. Повторите позже.");
            default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ошибка внешнего сервиса Trello.");
        };
    }
}
