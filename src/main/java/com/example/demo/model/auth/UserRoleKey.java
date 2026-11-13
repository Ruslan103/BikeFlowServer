package com.example.demo.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "user_account")
    private Long userAccount;

    @Column(name = "role")
    private Long role;
}
