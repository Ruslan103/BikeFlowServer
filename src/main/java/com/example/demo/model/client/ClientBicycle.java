package com.example.demo.model.client;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_bicycles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientBicycle extends AuditedEntity {
    @jakarta.persistence.Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version;

    public ClientBicycle(Client client) { this.client = client; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand", nullable = false)
    private BicycleBrand brand;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
