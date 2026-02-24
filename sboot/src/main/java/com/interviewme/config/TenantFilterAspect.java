package com.interviewme.config;

import com.interviewme.common.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* com.interviewme..repository..*(..))")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = entityManager.unwrap(Session.class);

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
            log.debug("Enabled tenant filter for tenantId: {}", tenantId);
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
