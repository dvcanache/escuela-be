package com.escuela.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Almacena el ID del Tenant actual en el hilo de ejecución (ThreadLocal).
 */
public class TenantContext {
    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        logger.debug("Setting current tenant to: {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
