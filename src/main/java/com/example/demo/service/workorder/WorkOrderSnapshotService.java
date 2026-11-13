package com.example.demo.service.workorder;

import com.example.demo.dto.WorkOrderRequest;
import com.example.demo.model.client.ClientType;
import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.model.workorder.WorkOrderClientSnapshot;
import com.example.demo.repository.ClientBicycleRepository;
import com.example.demo.repository.ClientContactRepository;
import com.example.demo.repository.ClientRepository;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class WorkOrderSnapshotService {
    private final ClientRepository clients;
    private final ClientBicycleRepository bicycles;
    private final ClientContactRepository contacts;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public com.example.demo.dto.client.ClientSelectionResponse preview(com.example.demo.dto.client.ClientSelectionRequest request) {
        var client = clients.findById(request.clientId()).orElseThrow(() -> missing("Клиент"));
        var bicycle = request.bicycleId() == null ? null : bicycles.findById(request.bicycleId()).orElseThrow(() -> missing("Велосипед"));
        var contact = request.contactId() == null ? null : contacts.findById(request.contactId()).orElseThrow(() -> missing("Контакт"));
        if (bicycle != null && !Objects.equals(bicycle.getClient().getId(), client.getId())) throw invalid("Велосипед принадлежит другому клиенту.");
        if (contact != null && !Objects.equals(contact.getClient().getId(), client.getId())) throw invalid("Контакт принадлежит другому клиенту.");
        return new com.example.demo.dto.client.ClientSelectionResponse(capture(client, bicycle, contact), client.getVersion(), bicycle == null ? null : bicycle.getVersion());
    }

    private WorkOrderClientSnapshot capture(com.example.demo.model.client.Client client,
            com.example.demo.model.client.ClientBicycle bicycle, com.example.demo.model.client.ClientContact contact) {
        String name = client.getType() == ClientType.ORGANIZATION ? client.getOrganizationName()
                : Stream.of(client.getLastName(), client.getFirstName(), client.getMiddleName())
                        .filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" "));
        if (name == null || name.isBlank()) name = contact != null ? contact.getValue() : "Клиент № " + client.getPublicId();
        return new WorkOrderClientSnapshot(client.getType().name(), name, client.getFirstName(), client.getLastName(),
                client.getMiddleName(), client.getOrganizationName(), client.getRepresentative(), contact == null ? null : contact.getType().getCode(),
                contact == null ? null : contact.getValue(), bicycle == null ? null : bicycle.getBrand().getName(),
                bicycle == null ? null : bicycle.getComment(), client.getComment());
    }

    public void apply(WorkOrder order, WorkOrderRequest request) {
        if (request.clientId() == null) throw invalid("Выберите клиента.");
        if (request.clientBicycleId() == null && request.customerParts().isEmpty()) {
            throw invalid("Добавьте велосипед или деталь клиента, принятую в ремонт.");
        }
        var client = clients.findById(request.clientId()).orElseThrow(() -> missing("Клиент"));
        var bicycle = request.clientBicycleId() == null ? null
                : bicycles.findById(request.clientBicycleId()).orElseThrow(() -> missing("Велосипед"));
        if (bicycle != null && !Objects.equals(bicycle.getClient().getId(), client.getId())) {
            throw invalid("Велосипед принадлежит другому клиенту.");
        }
        var contact = request.clientContactId() == null ? null : contacts.findById(request.clientContactId())
                .orElseThrow(() -> missing("Контакт"));
        if (contact != null && !Objects.equals(contact.getClient().getId(), client.getId())) throw invalid("Контакт принадлежит другому клиенту.");
        boolean refresh = order.getClientSnapshot() == null || request.snapshotAction() == WorkOrderRequest.SnapshotAction.REFRESH
                || order.getClient() == null || !Objects.equals(order.getClient().getId(), client.getId())
                || !Objects.equals(order.getClientBicycle() == null ? null : order.getClientBicycle().getId(),
                        bicycle == null ? null : bicycle.getId())
                || !Objects.equals(order.getClientContact() == null ? null : order.getClientContact().getId(), request.clientContactId());
        if (refresh) {
            entityManager.lock(client, jakarta.persistence.LockModeType.OPTIMISTIC);
            if (bicycle != null) entityManager.lock(bicycle, jakarta.persistence.LockModeType.OPTIMISTIC);
            if (!Objects.equals(client.getVersion(), request.expectedClientVersion())
                    || (bicycle != null && !Objects.equals(bicycle.getVersion(), request.expectedBicycleVersion()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Карточка клиента или велосипеда изменена. Перечитайте данные перед обновлением бланка.");
            }
            order.setClientSnapshot(capture(client, bicycle, contact));
        }
        order.setClient(client);
        order.setClientBicycle(bicycle);
        order.setClientContact(contact);
        order.setPhoneContact(null);
        order.setOtherContact(null);
        order.setContactMethod(order.getClientSnapshot().contactTypeCode());
        order.setContactValue(order.getClientSnapshot().contactValue());
    }
    private ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, text); }
    private ResponseStatusException missing(String text) { return new ResponseStatusException(HttpStatus.NOT_FOUND, text + " не найден."); }
}
