package com.example.demo.model.workorder;

import com.example.demo.model.audit.AuditedEntity;

import com.example.demo.model.service.Discount;
import com.example.demo.model.service.WorkshopService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order_services")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderServiceItem extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service", nullable = false)
    private WorkshopService service;

    @Column(name = "service_name_snapshot", nullable = false, length = 150)
    private String serviceNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount")
    private Discount discount;

    @Column(name = "price_without_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceWithoutDiscount;

    @Column(name = "price_with_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceWithDiscount;
}
