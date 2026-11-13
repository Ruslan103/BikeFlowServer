package com.example.demo.model.auth;

import com.example.demo.model.audit.AuditedEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole extends AuditedEntity {
    @EmbeddedId
    private UserRoleKey id;

    @MapsId("userAccount")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account", nullable = false)
    private UserAccount userAccount;

    @MapsId("role")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role", nullable = false)
    private Role role;

    public UserRole(UserAccount userAccount, Role role) {
        this.userAccount = userAccount;
        this.role = role;
        this.id = new UserRoleKey(userAccount.getId(), role.getId());
    }
}
