package com.example.demo.audit;

import com.example.demo.config.JpaAuditingConfig;
import com.example.demo.model.audit.AuditedEntity;
import com.example.demo.model.auth.Role;
import com.example.demo.model.auth.UserAccount;
import com.example.demo.model.auth.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(JpaAuditingTests.Config.class)
class JpaAuditingTests {
    @Configuration
    @EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
    @EnableJpaRepositories(basePackages = "com.example.demo.repository")
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.example.demo.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean
        EntityManager entityManager(EntityManagerFactory factory) {
            return SharedEntityManagerCreator.createSharedEntityManager(factory);
        }
        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean
        AuditorAware<UserAccount> currentUserAuditor(EntityManager entityManager) {
            return new JpaAuditingConfig().currentUserAuditor(entityManager);
        }
    }

    @Autowired EntityManager entityManager;
    @Autowired JpaTransactionManager transactionManager;
    TransactionTemplate transactions;
    Long firstActor;
    Long secondActor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        transactions = new TransactionTemplate(transactionManager);
        firstActor = transactions.execute(status -> persistUser("first-" + System.nanoTime()).getId());
        secondActor = transactions.execute(status -> persistUser("second-" + System.nanoTime()).getId());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creationAndUpdateUseSecurityPrincipalAndPreserveCreator() {
        authenticate(firstActor);
        Long id = transactions.execute(status -> persistUser("target-" + System.nanoTime()).getId());
        transactions.executeWithoutResult(status -> {
            var user = entityManager.find(UserAccount.class, id);
            assertEquals(firstActor, user.getCreatedBy().getId());
            assertEquals(firstActor, user.getUpdatedBy().getId());
        });
        authenticate(secondActor);
        transactions.executeWithoutResult(status ->
                entityManager.find(UserAccount.class, id).setDisplayName("Changed"));
        transactions.executeWithoutResult(status -> {
            var user = entityManager.find(UserAccount.class, id);
            assertEquals(firstActor, user.getCreatedBy().getId());
            assertEquals(secondActor, user.getUpdatedBy().getId());
        });
    }

    @Test
    void roleAssignmentsHaveTheirOwnAudit() {
        authenticate(firstActor);
        Long roleId = transactions.execute(status -> {
            entityManager.createNativeQuery(
                    "insert into roles (code, name) values ('AUDIT_TEST', 'Audit test')").executeUpdate();
            return entityManager.createQuery("select r.id from Role r where r.code = 'AUDIT_TEST'", Long.class)
                    .getSingleResult();
        });
        Long userId = transactions.execute(status -> {
            UserAccount target = newUser("with-role-" + System.nanoTime());
            target.setRoles(Set.of(entityManager.getReference(Role.class, roleId)));
            entityManager.persist(target);
            return target.getId();
        });
        transactions.executeWithoutResult(status -> {
            var link = entityManager.createQuery(
                    "select link from UserRole link where link.userAccount.id = :user", UserRole.class)
                    .setParameter("user", userId).getSingleResult();
            assertEquals(firstActor, link.getCreatedBy().getId());
            assertEquals(firstActor, link.getUpdatedBy().getId());
        });
        authenticate(secondActor);
        transactions.executeWithoutResult(status -> {
            var user = entityManager.find(UserAccount.class, userId);
            user.setRoles(Set.of(entityManager.getReference(Role.class, roleId)));
            user.setDisplayName("Same role retained");
        });
        transactions.executeWithoutResult(status -> {
            var link = entityManager.createQuery(
                    "select link from UserRole link where link.userAccount.id = :user", UserRole.class)
                    .setParameter("user", userId).getSingleResult();
            assertEquals(firstActor, link.getCreatedBy().getId());
        });
    }

    @Test
    void systemAndAnonymousWritesHaveNoInventedAuthor() {
        transactions.executeWithoutResult(status -> {
            var user = persistUser("system-" + System.nanoTime());
            assertNull(user.getCreatedBy());
            assertNull(user.getUpdatedBy());
        });
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        transactions.executeWithoutResult(status -> {
            var user = persistUser("anonymous-" + System.nanoTime());
            assertNull(user.getCreatedBy());
            assertNull(user.getUpdatedBy());
        });
    }

    @Test
    void everyEntitySupportsAudit() {
        entityManager.getMetamodel().getEntities().forEach(entity ->
                assertTrue(AuditedEntity.class.isAssignableFrom(entity.getJavaType()), entity.getName()));
    }

    private void authenticate(Long id) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(id.toString(), null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private UserAccount persistUser(String login) {
        var user = newUser(login);
        entityManager.persist(user);
        return user;
    }

    private UserAccount newUser(String login) {
        var user = new UserAccount();
        user.setLogin(login);
        user.setPasswordHash("test-hash");
        return user;
    }
}
