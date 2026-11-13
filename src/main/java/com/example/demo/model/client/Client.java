package com.example.demo.model.client;

import com.example.demo.model.audit.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client extends AuditedEntity {

    public Client(Long publicId) { this.publicId = publicId; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "public_id", nullable = false, unique = true)
    private Long publicId;

    @Column(name = "client_comment", columnDefinition = "TEXT")
    private String comment;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    @org.hibernate.annotations.ColumnDefault("'PERSON'")
    private ClientType type = ClientType.PERSON;

    @Column(name = "organization_name", length = 250)
    private String organizationName;

    @Column(name = "representative", length = 250)
    private String representative;

    @jakarta.persistence.Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version;
}
