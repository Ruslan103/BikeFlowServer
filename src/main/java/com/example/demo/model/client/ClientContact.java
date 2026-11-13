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
@Table(name = "client_contacts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientContact extends AuditedEntity {

    public ClientContact(Client client) { this.client = client; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_type", nullable = false)
    private ContactType type;

    @Column(name = "contact_value", nullable = false, length = 255)
    private String value;

    @Column(name = "label", length = 100)
    private String label;
}
