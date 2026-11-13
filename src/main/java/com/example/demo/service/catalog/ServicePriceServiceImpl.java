package com.example.demo.service.catalog;

import com.example.demo.dto.service.CreateServicePriceRequest;
import com.example.demo.dto.service.ServicePriceResponse;
import com.example.demo.dto.service.UpdateServicePriceRequest;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ServicePriceServiceImpl implements ServicePriceService {
    private final WorkshopServiceRepository services;
    private final ServicePriceRepository prices;

    @Override
    @Transactional
    public ServicePriceResponse create(Long serviceId, CreateServicePriceRequest request) {
        WorkshopService service = services.findByIdForPriceUpdate(serviceId)
                .orElseThrow(() -> serviceNotFound(serviceId));
        LocalDate start = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        prices.findFirstByServiceIdOrderByValidFromDescIdDesc(serviceId).ifPresent(previous -> {
            if (!start.isAfter(previous.getValidFrom())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Price start date must be after the latest price start date: " + previous.getValidFrom());
            }
            LocalDate end = start.minusDays(1);
            if (previous.getValidTo() == null || previous.getValidTo().isAfter(end)) {
                previous.setValidTo(end);
            }
        });
        ServicePrice saved = prices.save(new ServicePrice(service, request.price(), start));
        log.info("Создана цена услуги, ID услуги {}, ID цены {}", serviceId, saved.getId());
        return toResponse(saved);
    }

    @Override
    public List<ServicePriceResponse> findAll(Long serviceId) {
        if (!services.existsById(serviceId)) throw serviceNotFound(serviceId);
        return prices.findByServiceIdOrderByValidFromDescIdDesc(serviceId).stream().map(this::toResponse).toList();
    }

    @Override
    public ServicePriceResponse findById(Long serviceId, Long priceId) {
        return prices.findByIdAndServiceId(priceId, serviceId).map(this::toResponse).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Service price not found: " + priceId));
    }

    @Override
    @Transactional
    public ServicePriceResponse update(Long serviceId, Long priceId, UpdateServicePriceRequest request) {
        log.info("Начато изменение цены услуги, ID услуги {}, ID цены {}", serviceId, priceId);
        services.findByIdForPriceUpdate(serviceId).orElseThrow(() -> serviceNotFound(serviceId));
        ServicePrice price = prices.findByIdAndServiceId(priceId, serviceId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Цена услуги не найдена: " + priceId));
        price.setPrice(request.price());
        ServicePriceResponse response = toResponse(prices.save(price));
        log.info("Цена услуги успешно изменена, ID услуги {}, ID цены {}", serviceId, priceId);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long serviceId, Long priceId) {
        log.info("Начато удаление цены услуги, ID услуги {}, ID цены {}", serviceId, priceId);
        services.findByIdForPriceUpdate(serviceId).orElseThrow(() -> serviceNotFound(serviceId));
        List<ServicePrice> servicePrices = prices.findByServiceIdOrderByValidFromDescIdDesc(serviceId);
        int deletedIndex = -1;
        for (int index = 0; index < servicePrices.size(); index++) {
            if (servicePrices.get(index).getId().equals(priceId)) {
                deletedIndex = index;
                break;
            }
        }
        if (deletedIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Цена услуги не найдена: " + priceId);
        }

        ServicePrice previous = deletedIndex + 1 < servicePrices.size()
                ? servicePrices.get(deletedIndex + 1) : null;
        ServicePrice next = deletedIndex > 0 ? servicePrices.get(deletedIndex - 1) : null;
        if (previous != null) {
            previous.setValidTo(next == null ? null : next.getValidFrom().minusDays(1));
            prices.save(previous);
        }

        prices.delete(servicePrices.get(deletedIndex));
        prices.flush();
        log.info("Цена услуги успешно удалена, ID услуги {}, ID цены {}", serviceId, priceId);
    }

    private ResponseStatusException serviceNotFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id);
    }

    private ServicePriceResponse toResponse(ServicePrice price) {
        return new ServicePriceResponse(price.getId(), price.getService().getId(), price.getPrice(),
                price.getValidFrom(), price.getValidTo());
    }
}
