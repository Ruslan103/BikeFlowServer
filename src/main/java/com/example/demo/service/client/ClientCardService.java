package com.example.demo.service.client;

import com.example.demo.dto.client.BicycleCardRequest;
import com.example.demo.dto.client.ClientCardRequest;
import com.example.demo.dto.client.ClientSearchResponse;
import com.example.demo.model.client.BicycleBrand;
import com.example.demo.model.client.Client;
import com.example.demo.model.client.ClientBicycle;
import com.example.demo.model.client.ClientContact;
import com.example.demo.model.client.ClientType;
import com.example.demo.repository.BicycleBrandRepository;
import com.example.demo.repository.ClientBicycleRepository;
import com.example.demo.repository.ClientContactRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.WorkOrderRepository;
import com.example.demo.repository.ContactTypeRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClientCardService {
    private final ClientRepository clients;
    private final ClientBicycleRepository bicycles;
    private final WorkOrderRepository workOrders;
    private final ClientContactRepository contacts;
    private final ContactTypeRepository types;
    private final BicycleBrandRepository brands;
    private final ClientSearchService search;
    private final jakarta.persistence.EntityManager entityManager;

    public ClientSearchResponse create(ClientCardRequest request) {
        validate(request);
        checkDuplicates(null, request);
        Client client = new Client(clients.nextPublicId());
        apply(client, request);
        client = clients.saveAndFlush(client);
        saveContacts(client, request);
        return search.findById(client.getId());
    }

    public ClientSearchResponse update(Long id, ClientCardRequest request) {
        Client client = requireClient(id);
        checkVersion(client.getVersion(), request.version());
        if (client.getType() != request.type()) throw invalid("Тип существующего клиента изменять нельзя.");
        validate(request);
        checkDuplicates(id, request);
        apply(client, request);
        // Контакты являются частью карточки: их изменение тоже повышает её версию.
        entityManager.lock(client, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        var existingContacts = contacts.findByClientIdOrderByIdAsc(client.getId());
        saveContacts(client, request);
        deleteOmittedContacts(request, existingContacts);
        clients.flush();
        return search.findById(id);
    }

    public ClientSearchResponse createBicycle(Long clientId, BicycleCardRequest request) {
        Client client = requireClient(clientId);
        ClientBicycle bicycle = new ClientBicycle(client);
        applyBicycle(bicycle, request);
        bicycles.saveAndFlush(bicycle);
        return search.findById(clientId);
    }

    public ClientSearchResponse updateBicycle(Long clientId, Long bicycleId, BicycleCardRequest request) {
        ClientBicycle bicycle = bicycles.findById(bicycleId).orElseThrow(() -> missing("Велосипед"));
        if (!Objects.equals(bicycle.getClient().getId(), clientId)) throw invalid("Велосипед принадлежит другому клиенту.");
        checkVersion(bicycle.getVersion(), request.version());
        applyBicycle(bicycle, request);
        bicycles.flush();
        return search.findById(clientId);
    }

    public ClientSearchResponse deleteBicycle(Long clientId, Long bicycleId, Long version) {
        requireClient(clientId);
        ClientBicycle bicycle = bicycles.findById(bicycleId).orElseThrow(() -> missing("\u0412\u0435\u043b\u043e\u0441\u0438\u043f\u0435\u0434"));
        if (!Objects.equals(bicycle.getClient().getId(), clientId)) throw invalid("\u0412\u0435\u043b\u043e\u0441\u0438\u043f\u0435\u0434 \u043f\u0440\u0438\u043d\u0430\u0434\u043b\u0435\u0436\u0438\u0442 \u0434\u0440\u0443\u0433\u043e\u043c\u0443 \u043a\u043b\u0438\u0435\u043d\u0442\u0443.");
        checkVersion(bicycle.getVersion(), version);
        if (workOrders.existsByClientBicycleId(bicycleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "\u0412\u0435\u043b\u043e\u0441\u0438\u043f\u0435\u0434 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f \u0432 \u0437\u0430\u043a\u0430\u0437-\u043d\u0430\u0440\u044f\u0434\u0435 \u0438 \u043d\u0435 \u043c\u043e\u0436\u0435\u0442 \u0431\u044b\u0442\u044c \u0443\u0434\u0430\u043b\u0451\u043d.");
        }
        bicycles.delete(bicycle);
        bicycles.flush();
        return search.findById(clientId);
    }

    public void delete(Long id) {
        log.info("Начато удаление карточки клиента");
        Client client = clients.findByIdForUpdate(id).orElseThrow(() -> missing("Клиент"));
        if (workOrders.existsByClientId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "У клиента есть бланки. Удаление карточки запрещено, чтобы сохранить историю заказов.");
        }

        contacts.deleteAllByClientId(id);
        bicycles.deleteAllByClientId(id);
        clients.delete(client);
        clients.flush();
        log.info("Карточка клиента успешно удалена");
    }

    private void validate(ClientCardRequest request) {
        if (request.type() == ClientType.ORGANIZATION && blank(request.organizationName())) throw invalid("Укажите название организации.");
        if (request.type() == ClientType.PERSON && blank(request.firstName()) && blank(request.lastName()) && request.contacts().isEmpty()) {
            throw invalid("Укажите имя, фамилию или хотя бы один контакт.");
        }
        var seen = new HashSet<Long>();
        request.contacts().forEach(contact -> {
            if (contact.id() != null && !seen.add(contact.id())) throw invalid("Контакт указан несколько раз.");
        });
    }

    private void checkDuplicates(Long clientId, ClientCardRequest request) {
        if (request.allowDuplicateContacts()) return;
        for (var input : request.contacts()) {
            var type = types.findById(input.typeId()).orElseThrow(() -> invalid("Неизвестный тип контакта."));
            if (contacts.existsMatchingOtherClient(type.getCode(), input.value().strip(), clientId == null ? -1L : clientId)) {
                throw new ClientCardConflict("DUPLICATE_CONTACT", "Контакт уже есть у другого клиента. Выберите его или подтвердите создание отдельной карточки.", input.value().strip());
            }
        }
    }

    private void saveContacts(Client client, ClientCardRequest request) {
        for (var input : request.contacts()) {
            ClientContact contact = input.id() == null ? new ClientContact(client)
                    : contacts.findById(input.id()).orElseThrow(() -> missing("Контакт"));
            if (!Objects.equals(contact.getClient().getId(), client.getId())) throw invalid("Контакт принадлежит другому клиенту.");
            contact.setType(types.findById(input.typeId()).orElseThrow(() -> invalid("Неизвестный тип контакта.")));
            contact.setValue(input.value().strip());
            contact.setLabel(input.label());
            contacts.save(contact);
        }
        contacts.flush();
    }

    private void deleteOmittedContacts(ClientCardRequest request, List<ClientContact> existingContacts) {
        var retainedIds = request.contacts().stream()
                .map(ClientCardRequest.ContactInput::id)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        var removed = existingContacts.stream()
                .filter(contact -> !retainedIds.contains(contact.getId()))
                .toList();
        if (removed.isEmpty()) return;

        var removedIds = removed.stream().map(ClientContact::getId).toList();
        // Orders keep their copied contact snapshot; clear only the live foreign keys.
        workOrders.clearContactReferences(removedIds);
        contacts.deleteAll(removed);
        contacts.flush();
    }

    private void apply(Client client, ClientCardRequest request) {
        client.setType(request.type());
        client.setFirstName(request.type() == ClientType.PERSON ? trim(request.firstName()) : null);
        client.setLastName(request.type() == ClientType.PERSON ? trim(request.lastName()) : null);
        client.setMiddleName(request.type() == ClientType.PERSON ? trim(request.middleName()) : null);
        client.setOrganizationName(request.type() == ClientType.ORGANIZATION ? trim(request.organizationName()) : null);
        client.setRepresentative(request.type() == ClientType.ORGANIZATION ? trim(request.representative()) : null);
        client.setComment(request.comment());
    }
    private void applyBicycle(ClientBicycle bicycle, BicycleCardRequest request) {
        String name = request.brandName().strip();
        bicycle.setBrand(brands.findByNameIgnoreCase(name).orElseGet(() -> brands.save(new BicycleBrand(name))));
        bicycle.setComment(request.comment());
    }
    private Client requireClient(Long id) { return clients.findById(id).orElseThrow(() -> missing("Клиент")); }
    private void checkVersion(Long actual, Long expected) {
        if (expected == null || !Objects.equals(actual, expected)) throw new ClientCardConflict("STALE_CARD",
                "Карточка изменена другим пользователем. Перечитайте данные перед сохранением.");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.strip(); }
    private ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, text); }
    private ResponseStatusException missing(String text) { return new ResponseStatusException(HttpStatus.NOT_FOUND, text + " не найден."); }
}
