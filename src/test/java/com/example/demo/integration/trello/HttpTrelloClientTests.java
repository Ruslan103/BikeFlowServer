package com.example.demo.integration.trello;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpTrelloClientTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.trello.com/1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final HttpTrelloClient client = new HttpTrelloClient(builder.build(), () -> new TrelloCredentials("test-key", "test-token"));

    @Test
    void sendsCredentialsInHeaderAndReadsMember() {
        server.expect(requestTo("https://api.trello.com/1/members/me?fields=username,fullName"))
                .andExpect(header("Authorization", "OAuth oauth_consumer_key=\"test-key\", oauth_token=\"test-token\""))
                .andRespond(withSuccess("{\"id\":\"1\",\"username\":\"bike\",\"fullName\":\"Bike\"}", MediaType.APPLICATION_JSON));
        assertEquals("bike", client.member().username());
        server.verify();
    }

    @Test
    void readsBoardCatalogAndCreatesCardWithoutSecretsInBody() {
        server.expect(requestTo("https://api.trello.com/1/members/me/boards?filter=open&fields=name,url"))
                .andRespond(withSuccess("[{\"id\":\"board\",\"name\":\"Workshop\",\"url\":\"https://trello.com/b/board\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.trello.com/1/cards"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("{\"idList\":\"list\",\"name\":\"Бланк\",\"desc\":\"marker\",\"idLabels\":[\"label\"]}"))
                .andRespond(withSuccess("{\"id\":\"card\",\"name\":\"Бланк\",\"url\":\"https://trello.com/c/abc\",\"closed\":false}", MediaType.APPLICATION_JSON));
        assertEquals("Workshop", client.boards().getFirst().name());
        assertEquals("card", client.createCard("list", "Бланк", "marker", List.of("label")).id());
        server.verify();
    }

    @Test
    void missingOrMalformedCredentialsDoNotCallTrello() {
        for (TrelloCredentials value : List.of(new TrelloCredentials("", "token"),
                new TrelloCredentials("key", ""), new TrelloCredentials("key", "token\"\r\n"))) {
            var missing = new HttpTrelloClient(builder.build(), () -> value);
            assertEquals(503, assertThrows(ResponseStatusException.class, missing::member).getStatusCode().value());
        }
        assertFalse(new TrelloCredentials("secret-key", "secret-token").toString().contains("secret"));
        server.verify();
    }

    @Test
    void upstreamErrorsAreSanitized() {
        for (int status : List.of(401, 403, 404, 429, 500)) {
            var localBuilder = RestClient.builder().baseUrl("https://api.trello.com/1");
            var localServer = MockRestServiceServer.bindTo(localBuilder).build();
            var localClient = new HttpTrelloClient(localBuilder.build(), () -> new TrelloCredentials("test-key", "test-token"));
            localServer.expect(requestTo("https://api.trello.com/1/members/me?fields=username,fullName"))
                    .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive upstream body test-token"));
            var error = assertThrows(ResponseStatusException.class, localClient::member);
            assertEquals(status == 404 ? 404 : status == 429 ? 503 : 502, error.getStatusCode().value());
            assertFalse(error.toString().contains("test-token"));
            assertNull(error.getCause());
        }
    }

    @Test
    void malformedJsonReturnsBadGateway() {
        server.expect(requestTo("https://api.trello.com/1/members/me?fields=username,fullName"))
                .andRespond(withSuccess("broken", MediaType.APPLICATION_JSON));
        assertEquals(502, assertThrows(ResponseStatusException.class, client::member).getStatusCode().value());
    }
}
