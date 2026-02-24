package com.interviewme.common.util;

/**
 * Thread-local tenant context for multi-tenant data isolation.
 * Set by JwtAuthenticationFilter on each request.
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    public static Long getTenantId() {
        return currentTenantId.get();
    }

    public static void clear() {
        currentTenantId.remove();
    }
}
