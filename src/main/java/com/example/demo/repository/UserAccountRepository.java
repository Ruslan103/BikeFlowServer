package com.example.demo.repository;

import com.example.demo.model.auth.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    @EntityGraph(attributePaths = "roleAssignments.role")
    Optional<UserAccount> findByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCase(String login);
    @Query("""
            select count(distinct assignment.userAccount) from UserRole assignment
            where assignment.userAccount.enabled = true and assignment.role.code = :code
            """)
    long countByEnabledTrueAndRolesCode(@Param("code") String code);
}
