package com.example.demo.model.workorder;

import com.example.demo.model.audit.AuditedEntity;

import com.example.demo.model.client.Client;
import com.example.demo.model.client.ClientBicycle;
import com.example.demo.model.client.ClientContact;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "work_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_orders_blank_number",
                columnNames = "blank_number"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class WorkOrder extends AuditedEntity {
    @jakarta.persistence.Embedded
    private WorkOrderClientSnapshot clientSnapshot;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blank_number", nullable = false, length = 32)
    private String blankNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_bicycle")
    private ClientBicycle clientBicycle;

    @Column(name = "bicycle_condition_on_receipt", columnDefinition = "TEXT")
    private String bicycleConditionOnReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_contact")
    private ClientContact clientContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_contact")
    private ClientContact phoneContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_contact")
    private ClientContact otherContact;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reported_issue", columnDefinition = "TEXT")
    private String reportedIssue;

    @Column(name = "client_comment", columnDefinition = "TEXT")
    private String clientComment;

    @Column(name = "internal_comment", columnDefinition = "TEXT")
    private String internalComment;

    @Column(name = "contact_method", length = 30)
    private String contactMethod;

    @Column(name = "contact_value", length = 255)
    private String contactValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status", nullable = false)
    private WorkOrderStatus status;

    @Column(name = "trello_card", unique = true, length = 64)
    private String trelloCard;

    @Column(name = "trello_url", length = 2048)
    private String trelloUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trello_sync_status", nullable = false)
    private TrelloSyncStatus trelloSyncStatus;

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
