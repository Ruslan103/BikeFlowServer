package com.example.demo.model.auth;

import com.example.demo.model.audit.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login", nullable = false, unique = true, length = 100)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "security_version", nullable = false)
    private long securityVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "userAccount", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<UserRole> roleAssignments = new HashSet<>();

    public Set<Role> getRoles() {
        return roleAssignments.stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    public void setRoles(Set<Role> roles) {
        roleAssignments.removeIf(assignment -> roles.stream()
                .noneMatch(role -> sameRole(assignment.getRole(), role)));
        for (Role role : roles) {
            if (roleAssignments.stream().noneMatch(assignment -> sameRole(assignment.getRole(), role))) {
                roleAssignments.add(new UserRole(this, role));
            }
        }
    }

    private boolean sameRole(Role first, Role second) {
        return first == second || (first.getId() != null && Objects.equals(first.getId(), second.getId()));
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
