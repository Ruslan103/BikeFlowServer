package com.example.demo.service.workorder;

import com.example.demo.service.client.ClientSearchService;
import com.example.demo.dto.workorder.WorkOrderDetails;
import com.example.demo.dto.WorkOrderRequest;
import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.dto.WorkOrderServiceItemRequest;
import com.example.demo.dto.WorkOrderServiceItemResponse;
import com.example.demo.dto.product.WorkOrderCustomerPartRequest;
import com.example.demo.dto.product.WorkOrderCustomerPartResponse;
import com.example.demo.dto.product.WorkOrderProductItemRequest;
import com.example.demo.dto.product.WorkOrderProductItemResponse;
import com.example.demo.model.service.Discount;
import com.example.demo.model.service.ServicePrice;
import com.example.demo.model.service.WorkshopService;
import com.example.demo.model.product.Product;
import com.example.demo.model.product.ProductPrice;
import com.example.demo.model.workorder.TrelloSyncStatus;
import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.model.workorder.WorkOrderCustomerPart;
import com.example.demo.model.workorder.WorkOrderProductItem;
import com.example.demo.model.workorder.WorkOrderServiceItem;
import com.example.demo.model.workorder.WorkOrderStatus;
import com.example.demo.repository.DiscountRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ServicePriceRepository;
import com.example.demo.repository.TrelloSyncStatusRepository;
import com.example.demo.repository.WorkOrderRepository;
import com.example.demo.repository.WorkOrderServiceItemRepository;
import com.example.demo.repository.WorkOrderCustomerPartRepository;
import com.example.demo.repository.WorkOrderProductItemRepository;
import com.example.demo.repository.ProductPriceRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.WorkOrderStatusRepository;
import com.example.demo.repository.WorkshopServiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final String NEW_STATUS_CODE = "NEW";
    private static final String PENDING_TRELLO_STATUS_CODE = "PENDING";

    private final WorkOrderRepository workOrderRepository;
    private final ClientRepository clientRepository;
    private final WorkOrderStatusRepository workOrderStatusRepository;
    private final TrelloSyncStatusRepository trelloSyncStatusRepository;
    private final WorkshopServiceRepository workshopServiceRepository;
    private final DiscountRepository discountRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final WorkOrderServiceItemRepository workOrderServiceItemRepository;
    private final com.example.demo.service.catalog.ProductCatalogService productCatalog;
    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;
    private final WorkOrderProductItemRepository workOrderProductItemRepository;
    private final WorkOrderCustomerPartRepository workOrderCustomerPartRepository;
    private final WorkOrderNumberService workOrderNumberService;
    private final WorkOrderSnapshotService snapshots;
    private final ClientSearchService clientSearch;

    @Override
    public List<WorkOrderResponse> findAll() {
        log.info("Начато получение списка бланков");
        return workOrderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<WorkOrderResponse> findByNumber(String blankNumber) {
        log.info("Начат поиск бланка по номеру");
        String number = blankNumber.strip();
        if (number.matches("[0-9]+")) number = number.replaceFirst("^0+(?!$)", "");
        List<WorkOrder> orders = number.matches("[0-9]+")
                ? workOrderRepository.findByNormalizedBlankNumber(number)
                : workOrderRepository.findByBlankNumber(number).stream().toList();
        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> findByClientId(Long clientId) {
        log.info("Начато получение бланков клиента");
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клиент не найден: " + clientId);
        }
        return workOrderRepository.findAllByClientId(clientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        log.info("Начато создание бланка");
        WorkOrder order = new WorkOrder();
        order.setBlankNumber(request.blankNumber() == null || request.blankNumber().isBlank()
                ? workOrderNumberService.generate() : request.blankNumber().strip());
        if (workOrderRepository.existsByBlankNumber(order.getBlankNumber())) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Код бланка уже используется.");
        order.setTrelloSyncStatus(findTrelloSyncStatus(PENDING_TRELLO_STATUS_CODE));
        applyRequest(order, request);
        workOrderRepository.save(order);
        replaceServices(order, request.services());
        replaceProducts(order, request.products());
        replaceCustomerParts(order, request.customerParts());

        log.info("Бланк успешно создан");
        return toResponse(order);
    }

    @Override
    @Transactional
    public WorkOrderResponse update(String blankNumber, WorkOrderRequest request) {
        log.info("Начато обновление бланка");
        WorkOrder order = findOrderForUpdate(blankNumber);
        applyRequest(order, request);
        replaceServices(order, request.services());
        replaceProducts(order, request.products());
        replaceCustomerParts(order, request.customerParts());

        WorkOrderResponse response = toResponse(workOrderRepository.save(order));
        log.info("Бланк успешно обновлён");
        return response;
    }

    @Override
    @Transactional
    public void delete(String blankNumber) {
        log.info("Начато удаление бланка");
        WorkOrder order = findOrderForUpdate(blankNumber);
        workOrderServiceItemRepository.deleteAllByWorkOrderId(order.getId());
        workOrderProductItemRepository.deleteAllByWorkOrderId(order.getId());
        workOrderCustomerPartRepository.deleteAllByWorkOrderId(order.getId());
        workOrderRepository.delete(order);
        log.info("Бланк успешно удалён");
    }

    @Override
    public List<WorkOrderProductItemResponse> findProducts(String blankNumber) {
        WorkOrder order = findOrder(blankNumber);
        return workOrderProductItemRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId()).stream()
                .map(this::toProductItemResponse).toList();
    }

    @Override
    public WorkOrderProductItemResponse findProduct(String blankNumber, Long itemId) {
        WorkOrder order = findOrder(blankNumber);
        return findProductItem(order.getId(), itemId).map(this::toProductItemResponse)
                .orElseThrow(() -> notFound("Work order product", itemId));
    }

    @Override
    @Transactional
    public WorkOrderProductItemResponse addProduct(String blankNumber, WorkOrderProductItemRequest request) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        return toProductItemResponse(workOrderProductItemRepository.save(createProductItem(order, request, false)));
    }

    @Override
    @Transactional
    public WorkOrderProductItemResponse updateProduct(
            String blankNumber, Long itemId, WorkOrderProductItemRequest request) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        WorkOrderProductItem item = findProductItem(order.getId(), itemId)
                .orElseThrow(() -> notFound("Work order product", itemId));
        boolean keepInactiveProduct = item.getProduct().getId().equals(request.productId());
        WorkOrderProductItem updated = createProductItem(order, request, keepInactiveProduct);
        item.setSupplyStatus(updated.getSupplyStatus());
        item.setProduct(updated.getProduct());
        item.setProductNameSnapshot(updated.getProductNameSnapshot());
        item.setProductArticleSnapshot(updated.getProductArticleSnapshot());
        item.setProductUnitSnapshot(updated.getProductUnitSnapshot());
        item.setQuantity(updated.getQuantity());
        item.setDiscount(updated.getDiscount());
        item.setPriceWithoutDiscount(updated.getPriceWithoutDiscount());
        item.setPriceWithDiscount(updated.getPriceWithDiscount());
        return toProductItemResponse(workOrderProductItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteProduct(String blankNumber, Long itemId) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        WorkOrderProductItem item = findProductItem(order.getId(), itemId)
                .orElseThrow(() -> notFound("Work order product", itemId));
        workOrderProductItemRepository.delete(item);
    }

    @Override
    public List<WorkOrderCustomerPartResponse> findCustomerParts(String blankNumber) {
        WorkOrder order = findOrder(blankNumber);
        return workOrderCustomerPartRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId()).stream()
                .map(this::toCustomerPartResponse).toList();
    }

    @Override
    public WorkOrderCustomerPartResponse findCustomerPart(String blankNumber, Long partId) {
        WorkOrder order = findOrder(blankNumber);
        return findCustomerPartEntity(order.getId(), partId).map(this::toCustomerPartResponse)
                .orElseThrow(() -> notFound("Customer part", partId));
    }

    @Override
    @Transactional
    public WorkOrderCustomerPartResponse addCustomerPart(
            String blankNumber, WorkOrderCustomerPartRequest request) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        return toCustomerPartResponse(workOrderCustomerPartRepository.save(createCustomerPart(order, request)));
    }

    @Override
    @Transactional
    public WorkOrderCustomerPartResponse updateCustomerPart(
            String blankNumber, Long partId, WorkOrderCustomerPartRequest request) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        WorkOrderCustomerPart part = findCustomerPartEntity(order.getId(), partId)
                .orElseThrow(() -> notFound("Customer part", partId));
        WorkOrderCustomerPart updated = createCustomerPart(order, request);
        part.setName(updated.getName());
        part.setBrand(updated.getBrand());
        part.setModel(updated.getModel());
        part.setSerialNumber(updated.getSerialNumber());
        part.setQuantity(updated.getQuantity());
        part.setConditionOnReceipt(updated.getConditionOnReceipt());
        part.setReportedIssue(updated.getReportedIssue());
        part.setIncludedAccessories(updated.getIncludedAccessories());
        return toCustomerPartResponse(workOrderCustomerPartRepository.save(part));
    }

    @Override
    @Transactional
    public void deleteCustomerPart(String blankNumber, Long partId) {
        WorkOrder order = findOrderForUpdate(blankNumber);
        WorkOrderCustomerPart part = findCustomerPartEntity(order.getId(), partId)
                .orElseThrow(() -> notFound("Customer part", partId));
        if (order.getClientBicycle() == null
                && workOrderCustomerPartRepository.countByWorkOrderId(order.getId()) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Нельзя удалить последнюю принятую деталь: у заказа нет велосипеда.");
        }
        workOrderCustomerPartRepository.delete(part);
    }

    private void applyRequest(WorkOrder order, WorkOrderRequest request) {
        if (request.details() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Изменение клиента и велосипеда выполняется в их карточках, отдельно от бланка.");
        if (request.blankNumber() != null && order.getId() != null) {
            if (request.blankNumber().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите код бланка.");
            String number = request.blankNumber().strip();
            if (workOrderRepository.existsByBlankNumberAndIdNot(number, order.getId())) throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Код бланка уже используется.");
            order.setBlankNumber(number);
        }
        if (request.createdAt() != null) order.setCreatedAt(request.createdAt());
        snapshots.apply(order, request);
        order.setBicycleConditionOnReceipt(order.getClientBicycle() == null ? null : request.bicycleConditionOnReceipt());
        order.setReportedIssue(request.reportedIssue());
        order.setClientComment(request.clientComment());
        order.setInternalComment(request.internalComment());
        order.setStatus(findWorkOrderStatus(request.statusCode()));
    }

    private void replaceServices(WorkOrder order, List<WorkOrderServiceItemRequest> services) {
        workOrderServiceItemRepository.deleteAllByWorkOrderId(order.getId());

        List<WorkOrderServiceItem> serviceItems = services.stream()
                .map(serviceRequest -> createServiceItem(order, serviceRequest))
                .toList();
        workOrderServiceItemRepository.saveAll(serviceItems);
    }

    private void replaceProducts(WorkOrder order, List<WorkOrderProductItemRequest> products) {
        var existingProductIds = workOrderProductItemRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId())
                .stream().map(item -> item.getProduct().getId()).collect(java.util.stream.Collectors.toSet());
        workOrderProductItemRepository.deleteAllByWorkOrderId(order.getId());
        List<WorkOrderProductItem> items = products.stream()
                .map(productRequest -> createProductItem(order, productRequest,
                        existingProductIds.contains(productRequest.productId())))
                .toList();
        workOrderProductItemRepository.saveAll(items);
    }

    private WorkOrderProductItem createProductItem(
            WorkOrder order, WorkOrderProductItemRequest request, boolean allowInactive) {
        if ((request.productId() == null) == (request.newProduct() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите существующий или новый товар.");
        }
        Long productId = request.productId() == null ? productCatalog.create(request.newProduct()).id() : request.productId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> notFound("Product", request.productId()));
        if (!product.isActive() && !allowInactive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Товар отключён.");
        }

        BigDecimal priceWithoutDiscount = request.priceWithoutDiscount();
        if (priceWithoutDiscount == null) {
            ProductPrice price = productPriceRepository.findActivePrices(
                            product.getId(), LocalDate.now(), PageRequest.of(0, 1))
                    .stream().findFirst().orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Для товара нет действующей цены: " + product.getId()));
            priceWithoutDiscount = price.getPrice();
        }
        Discount discount = findDiscount(request.discountId());
        BigDecimal priceWithDiscount = request.priceWithDiscount();
        if (priceWithDiscount == null) {
            BigDecimal percentage = discount == null ? BigDecimal.ZERO : discount.getPercentage();
            priceWithDiscount = priceWithoutDiscount.multiply(BigDecimal.valueOf(100).subtract(percentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        WorkOrderProductItem item = new WorkOrderProductItem();
        item.setWorkOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot(product.getName());
        item.setProductArticleSnapshot(product.getArticle());
        item.setProductUnitSnapshot(product.getUnit());
        item.setSupplyStatus(request.supplyStatus() == null
                ? com.example.demo.model.workorder.ProductSupplyStatus.IN_STOCK : request.supplyStatus());
        item.setQuantity(request.quantity());
        item.setDiscount(discount);
        item.setPriceWithoutDiscount(priceWithoutDiscount.setScale(2, RoundingMode.UNNECESSARY));
        item.setPriceWithDiscount(priceWithDiscount.setScale(2, RoundingMode.UNNECESSARY));
        return item;
    }

    private void replaceCustomerParts(WorkOrder order, List<WorkOrderCustomerPartRequest> customerParts) {
        workOrderCustomerPartRepository.deleteAllByWorkOrderId(order.getId());
        List<WorkOrderCustomerPart> parts = customerParts.stream()
                .map(request -> createCustomerPart(order, request)).toList();
        workOrderCustomerPartRepository.saveAll(parts);
    }

    private WorkOrderCustomerPart createCustomerPart(WorkOrder order, WorkOrderCustomerPartRequest request) {
        WorkOrderCustomerPart part = new WorkOrderCustomerPart();
        part.setWorkOrder(order);
        part.setName(request.name().strip());
        part.setBrand(clean(request.brand()));
        part.setModel(clean(request.model()));
        part.setSerialNumber(clean(request.serialNumber()));
        part.setQuantity(request.quantity());
        part.setConditionOnReceipt(request.conditionOnReceipt());
        part.setReportedIssue(request.reportedIssue());
        part.setIncludedAccessories(request.includedAccessories());
        return part;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private WorkOrderServiceItem createServiceItem(
            WorkOrder order,
            WorkOrderServiceItemRequest request
    ) {
        WorkshopService service = workshopServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> notFound("Service", request.serviceId()));
        BigDecimal priceWithoutDiscount = request.priceWithoutDiscount();
        if (priceWithoutDiscount == null) {
            ServicePrice price = servicePriceRepository.findActivePrices(
                            service.getId(),
                            LocalDate.now(),
                            PageRequest.of(0, 1)
                    )
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No active price configured for service: " + request.serviceId()
                    ));
            priceWithoutDiscount = price.getPrice();
        }
        Discount discount = findDiscount(request.discountId());

        WorkOrderServiceItem item = new WorkOrderServiceItem();
        item.setWorkOrder(order);
        item.setService(service);
        item.setServiceNameSnapshot(request.serviceNameSnapshot() == null ? service.getName() : request.serviceNameSnapshot());
        item.setDiscount(discount);
        item.setPriceWithoutDiscount(priceWithoutDiscount);
        item.setPriceWithDiscount(request.priceWithDiscount());
        return item;
    }

    private Discount findDiscount(Long discountId) {
        if (discountId == null) {
            return null;
        }

        return discountRepository.findById(discountId)
                .orElseThrow(() -> notFound("Discount", discountId));
    }

    private WorkOrder findOrderForUpdate(String blankNumber) {
        String number = blankNumber.strip();
        var exact = workOrderRepository.findByNumberForUpdate(number);
        if (exact.isPresent()) return exact.get();
        List<WorkOrder> normalized = normalizedOrderNumbers(number);
        if (normalized.size() > 1) throw ambiguousOrderNumber(blankNumber);
        if (normalized.isEmpty()) throw orderNotFound(blankNumber);
        return workOrderRepository.findByNumberForUpdate(normalized.getFirst().getBlankNumber())
                .orElseThrow(() -> orderNotFound(blankNumber));
    }

    private WorkOrder findOrder(String blankNumber) {
        String number = blankNumber.strip();
        var exact = workOrderRepository.findByBlankNumber(number);
        if (exact.isPresent()) return exact.get();
        List<WorkOrder> normalized = normalizedOrderNumbers(number);
        if (normalized.size() > 1) throw ambiguousOrderNumber(blankNumber);
        return normalized.stream().findFirst().orElseThrow(() -> orderNotFound(blankNumber));
    }

    private List<WorkOrder> normalizedOrderNumbers(String number) {
        if (!number.matches("[0-9]+")) return List.of();
        String normalized = number.replaceFirst("^0+(?!$)", "");
        return workOrderRepository.findByNormalizedBlankNumber(normalized);
    }

    private ResponseStatusException orderNotFound(String blankNumber) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found: " + blankNumber);
    }

    private ResponseStatusException ambiguousOrderNumber(String blankNumber) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Work order number is ambiguous: " + blankNumber);
    }

    private java.util.Optional<WorkOrderProductItem> findProductItem(Long workOrderId, Long itemId) {
        return workOrderProductItemRepository.findByIdAndWorkOrderId(itemId, workOrderId);
    }

    private java.util.Optional<WorkOrderCustomerPart> findCustomerPartEntity(Long workOrderId, Long partId) {
        return workOrderCustomerPartRepository.findByIdAndWorkOrderId(partId, workOrderId);
    }
    private WorkOrderStatus findWorkOrderStatus(String code) {
        return workOrderStatusRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown work order status: " + code
                ));
    }

    private TrelloSyncStatus findTrelloSyncStatus(String code) {
        return trelloSyncStatusRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Trello sync status is not configured: " + code
                ));
    }

    private ResponseStatusException notFound(String entityName, Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                entityName + " not found: " + id
        );
    }

    private WorkOrderResponse toResponse(WorkOrder order) {
        return new WorkOrderResponse(
                order.getBlankNumber(),
                order.getClient().getId(),
                order.getClientBicycle() == null ? null : order.getClientBicycle().getId(),
                order.getClientContact() == null ? null : order.getClientContact().getId(),
                order.getCreatedAt(),
                order.getReportedIssue(),
                order.getClientComment(),
                order.getInternalComment(),
                order.getContactMethod(),
                order.getContactValue(),
                order.getStatus().getCode(),
                order.getTrelloCard(),
                order.getTrelloUrl(),
                workOrderServiceItemRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId())
                        .stream()
                        .map(this::toServiceItemResponse)
                        .toList(),
                order.getTrelloSyncStatus().getCode(),
                clientSearch.findById(order.getClient().getId()),
                details(order), order.getClientSnapshot(),
                workOrderProductItemRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId()).stream()
                        .map(this::toProductItemResponse).toList(),
                workOrderCustomerPartRepository.findAllByWorkOrderIdOrderByIdAsc(order.getId()).stream()
                        .map(this::toCustomerPartResponse).toList(), order.getBicycleConditionOnReceipt()
        );
    }

    private WorkOrderDetails details(WorkOrder order) {
        var snapshot = order.getClientSnapshot();
        if (snapshot == null) return null;
        boolean phone = "PHONE".equalsIgnoreCase(snapshot.contactTypeCode());
        return new WorkOrderDetails(snapshot.firstName(), snapshot.lastName(), snapshot.middleName(),
                snapshot.bicycleBrand(), snapshot.bicycleComment(), null, phone ? snapshot.contactValue() : "",
                null, phone ? null : snapshot.contactTypeCode(), phone ? "" : snapshot.contactValue());
    }
    private WorkOrderServiceItemResponse toServiceItemResponse(WorkOrderServiceItem item) {
        Discount discount = item.getDiscount();
        return new WorkOrderServiceItemResponse(
                item.getService().getId(),
                item.getServiceNameSnapshot(),
                discount == null ? null : discount.getId(),
                discount == null ? null : discount.getName(),
                discount == null ? null : discount.getPercentage(),
                item.getPriceWithoutDiscount(),
                item.getPriceWithDiscount()
        );
    }

    private WorkOrderProductItemResponse toProductItemResponse(WorkOrderProductItem item) {
        Discount discount = item.getDiscount();
        BigDecimal lineTotal = item.getPriceWithDiscount().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        return new WorkOrderProductItemResponse(item.getId(), item.getProduct().getId(), item.getProductNameSnapshot(),
                item.getProductArticleSnapshot(), item.getProductUnitSnapshot(), item.getQuantity(),
                discount == null ? null : discount.getId(), discount == null ? null : discount.getName(),
                discount == null ? null : discount.getPercentage(), item.getPriceWithoutDiscount(),
                item.getPriceWithDiscount(), lineTotal, item.getSupplyStatus());
    }

    private WorkOrderCustomerPartResponse toCustomerPartResponse(WorkOrderCustomerPart part) {
        return new WorkOrderCustomerPartResponse(part.getId(), part.getName(), part.getBrand(), part.getModel(),
                part.getSerialNumber(), part.getQuantity(), part.getConditionOnReceipt(), part.getReportedIssue(),
                part.getIncludedAccessories());
    }
}
