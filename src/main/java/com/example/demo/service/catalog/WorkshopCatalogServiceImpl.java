package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateWorkshopServiceRequest;
import com.example.demo.dto.service.WorkshopServiceResponse;
import com.example.demo.dto.service.WorkshopServiceBatchResponse;
import com.example.demo.dto.service.UpdateWorkshopServiceRequest;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import com.example.demo.repository.WorkOrderServiceItemRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorkshopCatalogServiceImpl implements WorkshopCatalogService {
    private final WorkshopServiceRepository services;
    private final ServicePriceRepository prices;
    private final WorkOrderServiceItemRepository workOrderServiceItems;

    @Override
    @Transactional
    public WorkshopServiceResponse create(CreateWorkshopServiceRequest request) {
        log.info("Начато создание услуги");
        WorkshopService service = services.save(new WorkshopService(request.name().strip(), request.description()));
        LocalDate validFrom = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        ServicePrice price = prices.save(new ServicePrice(service, request.price(), validFrom));
        log.info("Услуга успешно создана, ID {}", service.getId());
        return toResponse(service, price);
    }

    @Override
    @Transactional
    public WorkshopServiceResponse update(Long id, UpdateWorkshopServiceRequest request) {
        log.info("Начато обновление услуги, ID {}", id);
        WorkshopService service = services.findByIdForPriceUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id));
        service.setName(request.name().strip());
        service.setDescription(request.description());
        WorkshopServiceResponse response = withCurrentPrice(services.save(service), LocalDate.now());
        log.info("Услуга успешно обновлена, ID {}", id);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Начато удаление услуги, ID {}", id);
        WorkshopService service = services.findByIdForPriceUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id));
        if (workOrderServiceItems.existsByServiceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Услуга используется в бланках и не может быть удалена.");
        }

        prices.deleteAllByServiceId(id);
        services.delete(service);
        services.flush();
        log.info("Услуга успешно удалена, ID {}", id);
    }

    @Override
    public WorkshopServiceResponse findById(Long id) {
        WorkshopService service = services.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id));
        return withCurrentPrice(service, LocalDate.now());
    }

    @Override
    public List<WorkshopServiceResponse> findAll() {
        LocalDate today = LocalDate.now();
        return services.findAll(Sort.by("name").and(Sort.by("id"))).stream()
                .map(service -> withCurrentPrice(service, today))
                .toList();
    }

    @Override
    public WorkshopServiceBatchResponse findBatch(int page, int size) {
        log.info("Начато получение пачки услуг: страница {}, размер {}", page, size);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Номер страницы должен быть неотрицательным, размер пачки — от 1 до 100.");
        }

        Page<WorkshopService> result = services.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"))));
        List<WorkshopServiceResponse> responses = withCurrentPrices(result.getContent(), LocalDate.now());
        return new WorkshopServiceBatchResponse(responses, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.hasNext(), result.hasPrevious());
    }

    @Override
    public List<WorkshopServiceResponse> searchByName(String query) {
        String normalizedQuery = query == null ? "" : query.strip();
        if (normalizedQuery.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название или часть названия услуги.");
        }
        if (normalizedQuery.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Запрос слишком длинный.");
        }

        log.info("Начат поиск услуг по названию");
        List<WorkshopService> matches = services.searchByName(normalizedQuery, PageRequest.of(0, 50));
        return withCurrentPrices(matches, LocalDate.now());
    }

    private WorkshopServiceResponse withCurrentPrice(WorkshopService service, LocalDate date) {
        ServicePrice price = prices.findActivePrices(service.getId(), date, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        return toResponse(service, price);
    }

    private List<WorkshopServiceResponse> withCurrentPrices(List<WorkshopService> services, LocalDate date) {
        if (services.isEmpty()) return List.of();
        Map<Long, ServicePrice> currentPrices = prices.findActivePricesForServices(
                        services.stream().map(WorkshopService::getId).toList(), date)
                .stream()
                .collect(Collectors.toMap(price -> price.getService().getId(), price -> price,
                        (first, ignored) -> first));
        return services.stream()
                .map(service -> toResponse(service, currentPrices.get(service.getId())))
                .toList();
    }

    private WorkshopServiceResponse toResponse(WorkshopService service, ServicePrice price) {
        return new WorkshopServiceResponse(service.getId(), service.getName(), service.getDescription(),
                price == null ? null : price.getPrice(), price == null ? null : price.getValidFrom(),
                price == null ? null : price.getValidTo());
    }
}
