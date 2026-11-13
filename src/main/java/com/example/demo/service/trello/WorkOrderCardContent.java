package com.example.demo.service.trello;

import com.example.demo.model.workorder.WorkOrder;
import com.example.demo.model.workorder.WorkOrderCustomerPart;
import com.example.demo.model.workorder.WorkOrderProductItem;
import com.example.demo.model.workorder.WorkOrderServiceItem;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class WorkOrderCardContent {
    private WorkOrderCardContent() { }

    static String marker(WorkOrder order) {
        return "[bike-work-order:" + order.getBlankNumber() + ":" + order.getCreatedAt() + "]";
    }

    static String title(WorkOrder order) {
        return "Бланк № " + order.getBlankNumber() + " · " + clientName(order);
    }

    static String description(WorkOrder order, List<WorkOrderServiceItem> services) {
        return description(order, services, List.of(), List.of());
    }

    static String description(WorkOrder order, List<WorkOrderServiceItem> services,
            List<WorkOrderProductItem> products, List<WorkOrderCustomerPart> customerParts) {
        var snapshot = order.getClientSnapshot();
        StringBuilder text = new StringBuilder("## Клиент\n").append(clientName(order));
        if (snapshot != null && snapshot.representative() != null && !snapshot.representative().isBlank()) {
            text.append("\nПредставитель: ").append(snapshot.representative());
        }
        text.append("\n\n## Контакт\n").append(value(snapshot == null ? order.getContactValue() : snapshot.contactValue()));
        if (snapshot != null && snapshot.bicycleBrand() != null) {
            text.append("\n\n## Велосипед\n").append(value(snapshot.bicycleBrand()))
                    .append('\n').append(value(snapshot.bicycleComment()));
        }
        text.append("\n\n## Услуги\n");
        if (services.isEmpty()) text.append("Нет услуг\n");
        services.forEach(line -> text.append("- ").append(value(line.getServiceNameSnapshot()))
                .append(" — стоимость: ").append(line.getPriceWithoutDiscount().toPlainString())
                .append("; со скидкой: ").append(line.getPriceWithDiscount().toPlainString()).append('\n'));
        text.append("\n## Товары мастерской\n");
        if (products.isEmpty()) text.append("Нет товаров\n");
        products.forEach(line -> text.append("- ").append(value(line.getProductNameSnapshot()))
                .append(" — ").append(line.getQuantity().toPlainString()).append(' ')
                .append(value(line.getProductUnitSnapshot())).append("; за единицу со скидкой: ")
                .append(line.getPriceWithDiscount().toPlainString()).append('\n'));
        text.append("\n## Детали клиента\n");
        if (customerParts.isEmpty()) text.append("Нет принятых деталей\n");
        customerParts.forEach(part -> text.append("- ").append(value(part.getName()))
                .append(" — ").append(part.getQuantity().toPlainString()).append('\n'));
        return text.append("\n## Комментарий для себя\n").append(value(order.getInternalComment()))
                .append("\n\n## Заявленная неисправность\n").append(value(order.getReportedIssue()))
                .append("\n\n").append(marker(order)).toString();
    }

    private static String clientName(WorkOrder order) {
        if (order.getClientSnapshot() != null) return order.getClientSnapshot().displayName();
        if (order.getClient() == null) return "";
        return Stream.of(order.getClient().getLastName(), order.getClient().getFirstName(), order.getClient().getMiddleName())
                .filter(part -> part != null && !part.isBlank()).collect(Collectors.joining(" "));
    }
    private static String value(String value) { return Objects.toString(value, ""); }
}
