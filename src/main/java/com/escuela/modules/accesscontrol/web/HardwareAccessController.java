package com.escuela.modules.accesscontrol.web;

import com.escuela.modules.accesscontrol.domain.AccessLog;
import com.escuela.modules.accesscontrol.service.AccessControlService;
import com.escuela.modules.security.domain.User;
import com.escuela.modules.tenant.domain.Tenant;
import com.escuela.core.common.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/hardware")
public class HardwareAccessController {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.escuela.modules.tenant.domain.TenantRepository tenantRepository;

    @PostMapping("/access")
    public ResponseEntity<?> recordAccess(@RequestBody HardwareAccessRequest request) {
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().body("User ID is required.");
        }

        AccessLog log = new AccessLog();
        log.setDeviceId(request.getDeviceId());
        log.setDirection(request.getDirection());
        log.setEventTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());

        // Fetch managed User
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));
        log.setUser(user);

        // Fetch managed Tenant
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("X-Tenant-ID header is missing or invalid.");
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
        log.setTenant(tenant);

        try {
            AccessLog processed = accessControlService.processAccess(log);
            return ResponseEntity.ok("Access granted and registered.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
