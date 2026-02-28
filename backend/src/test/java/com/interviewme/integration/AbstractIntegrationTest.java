package com.interviewme.integration;

import com.interviewme.common.dto.ai.LlmClient;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.common.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(classes = AbstractIntegrationTest.TestApplication.class)
@ActiveProfiles("integration")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18")
                .withDatabaseName("interviewme_test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("init-pgvector.sql");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @SpringBootApplication
    @ComponentScan(basePackages = "com.interviewme")
    @EntityScan(basePackages = "com.interviewme")
    @EnableJpaRepositories(basePackages = "com.interviewme")
    @EnableTransactionManagement
    static class TestApplication {

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }

        @Bean
        public LlmClient llmClient() {
            return request -> new LlmResponse("test response", 10, 100L);
        }
    }

    @Aspect
    @Component
    static class TestTenantFilterAspect {

        @PersistenceContext
        private EntityManager entityManager;

        @Around("execution(* com.interviewme..repository..*(..))")
        public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
            Session session = entityManager.unwrap(Session.class);
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
            }
            try {
                return joinPoint.proceed();
            } finally {
                if (tenantId != null) {
                    session.disableFilter("tenantFilter");
                }
            }
        }
    }
}
