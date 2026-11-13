package com.example.demo.model.workorder;

import com.example.demo.model.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Singleton setting used as the initial comment for new work orders. */
@Entity
@Table(name = "work_order_default_comments")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderDefaultComment extends AuditedEntity {
    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id = SINGLETON_ID;

    @Column(name = "default_comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
}
