package com.example.demo.model.workorder;

import com.example.demo.model.audit.AuditedEntity;
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
@Table(name = "work_order_customer_parts")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderCustomerPart extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "condition_on_receipt", columnDefinition = "TEXT")
    private String conditionOnReceipt;

    @Column(name = "reported_issue", columnDefinition = "TEXT")
    private String reportedIssue;

    @Column(name = "included_accessories", columnDefinition = "TEXT")
    private String includedAccessories;
}
