package com.example.demo.model.workorder;

import com.example.demo.model.audit.AuditedEntity;
import com.example.demo.model.product.Product;
import com.example.demo.model.service.Discount;
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
@Table(name = "work_order_products")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderProductItem extends AuditedEntity {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "supply_status", nullable = false, length = 20)
    private ProductSupplyStatus supplyStatus = ProductSupplyStatus.IN_STOCK;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product", nullable = false)
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 150)
    private String productNameSnapshot;

    @Column(name = "product_article_snapshot", length = 80)
    private String productArticleSnapshot;

    @Column(name = "product_unit_snapshot", nullable = false, length = 30)
    private String productUnitSnapshot;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount")
    private Discount discount;

    @Column(name = "price_without_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceWithoutDiscount;

    @Column(name = "price_with_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceWithDiscount;
}
