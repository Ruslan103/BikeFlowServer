package com.example.demo.service.client;

import com.example.demo.model.client.Client;
import com.example.demo.dto.client.ClientBicycleResponse;
import com.example.demo.dto.client.ClientContactResponse;
import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.dto.client.ClientBatchResponse;
import com.example.demo.repository.ClientBicycleRepository;
import com.example.demo.repository.ClientContactRepository;
import com.example.demo.repository.ClientRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ClientSearchServiceImpl implements ClientSearchService {
    private final ClientRepository clients;
    private final ClientBicycleRepository bicycles;
    private final ClientContactRepository contacts;
    private final boolean trigramSearchEnabled;

    @Autowired
    public ClientSearchServiceImpl(ClientRepository clients, ClientBicycleRepository bicycles,
                                   ClientContactRepository contacts, DataSource dataSource) {
        this.clients = clients;
        this.bicycles = bicycles;
        this.contacts = contacts;
        this.trigramSearchEnabled = isPostgres(dataSource);
    }

    public ClientSearchServiceImpl(ClientRepository clients, ClientBicycleRepository bicycles,
                                   ClientContactRepository contacts) {
        this.clients = clients;
        this.bicycles = bicycles;
        this.contacts = contacts;
        this.trigramSearchEnabled = false;
    }

    private boolean isPostgres(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException exception) {
            log.warn("Не удалось определить тип БД для поиска с опечатками: {}",
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public ClientSearchResponse findById(Long id) {
        return toResponses(List.of(clients.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")))).getFirst();
    }

    @Override
    public ClientBatchResponse findBatch(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Номер страницы должен быть неотрицательным, размер пачки — от 1 до 100.");
        }

        Page<Client> result = clients.findAllAlphabetically(PageRequest.of(page, size));
        return new ClientBatchResponse(
                toResponses(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Override
    public List<ClientSearchResponse> findByPhone(String phone) {
        String suffix = phoneSuffix(phone);
        var found = clients.findByPhoneSuffix(suffix);
        return toResponses(found);
    }

    private List<ClientSearchResponse> toResponses(List<Client> found) {
        if (found.isEmpty()) return List.of();
        var ids = found.stream().map(client -> client.getId()).toList();
        var clientBicycles = bicycles.findByClientIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(bicycle -> bicycle.getClient().getId(),
                        Collectors.mapping(bicycle -> new ClientBicycleResponse(bicycle.getId(),
                                bicycle.getBrand().getId(), bicycle.getBrand().getName(), bicycle.getComment(), bicycle.getVersion()),
                                Collectors.toList())));
        var clientContacts = contacts.findByClientIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(contact -> contact.getClient().getId(),
                        Collectors.mapping(contact -> new ClientContactResponse(contact.getId(),
                                contact.getType().getId(), contact.getType().getCode(), contact.getType().getName(),
                                contact.getValue(), contact.getLabel()), Collectors.toList())));
        return found.stream().map(client -> new ClientSearchResponse(client.getId(), client.getPublicId(),
                client.getLastName(), client.getFirstName(), client.getMiddleName(), client.getComment(),
                clientBicycles.getOrDefault(client.getId(), List.of()),
                clientContacts.getOrDefault(client.getId(), List.of()), client.getType(), client.getOrganizationName(),
                client.getRepresentative(), client.getVersion())).toList();
    }

    public com.example.demo.dto.client.ClientSearchResult search(String query) {
        String text = query == null ? "" : query.strip();
        if (text.isEmpty()) return new com.example.demo.dto.client.ClientSearchResult(
                toResponses(clients.findAll(org.springframework.data.domain.Sort.by("id").descending())), List.of());
        if (text.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слишком длинный запрос.");
        if (text.matches("[+0-9()\\s-]+")) {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return new com.example.demo.dto.client.ClientSearchResult(List.of(), List.of());
            var exact = clients.findByExactPhone(digits);
            var exactIds = exact.stream().map(Client::getId).collect(Collectors.toSet());
            var similarBySuffix = digits.length() > 3 ? clients.findByPhoneSuffix(digits.substring(3)) : List.<Client>of();
            List<Client> similar = similarBySuffix.stream()
                    .filter(client -> !exactIds.contains(client.getId()))
                    .collect(Collectors.toCollection(java.util.ArrayList::new));
            var similarIds = similar.stream().map(Client::getId).collect(Collectors.toSet());
            if (trigramSearchEnabled && digits.length() >= 7) {
                clients.findBySimilarPhoneSuffix(digits.substring(3)).stream()
                        .filter(client -> !exactIds.contains(client.getId()) && similarIds.add(client.getId()))
                        .forEach(similar::add);
            }
            return new com.example.demo.dto.client.ClientSearchResult(toResponses(exact), toResponses(similar), exact.size()==1);
        }
        List<Client> exact = clients.searchText(text);
        var exactIds = exact.stream().map(Client::getId).collect(Collectors.toSet());
        List<Client> similar = trigramSearchEnabled && text.length() >= 4 ? clients.searchTextWithTypos(text).stream()
                .filter(client -> !exactIds.contains(client.getId())).toList() : List.of();
        return new com.example.demo.dto.client.ClientSearchResult(toResponses(exact), toResponses(similar));
    }

    private String phoneSuffix(String phone) {
        if (phone == null || phone.isBlank() || phone.length() > 255 || !phone.matches("[+0-9()\\s-]+")) {
            throw invalidPhone();
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 3) throw invalidPhone();
        return digits.substring(3);
    }

    private ResponseStatusException invalidPhone() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Phone must contain at least four digits and only phone formatting characters");
    }
}
