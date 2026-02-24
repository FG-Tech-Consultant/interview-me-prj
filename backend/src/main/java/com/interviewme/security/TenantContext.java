package com.interviewme.security;

/**
 * @deprecated Use {@link com.interviewme.common.util.TenantContext} directly.
 * Kept for backward compatibility during migration.
 */
@Deprecated
public class TenantContext {

    public static void setTenantId(Long tenantId) {
        com.interviewme.common.util.TenantContext.setTenantId(tenantId);
    }

    public static Long getTenantId() {
        return com.interviewme.common.util.TenantContext.getTenantId();
    }

    public static void clear() {
        com.interviewme.common.util.TenantContext.clear();
    }
}
