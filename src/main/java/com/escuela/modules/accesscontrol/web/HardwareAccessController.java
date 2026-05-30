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

    @PostMapping("/access")
    public ResponseEntity<?> recordAccess(@RequestBody HardwareAccessRequest request) {
        AccessLog log = new AccessLog();
        log.setDeviceId(request.getDeviceId());
        log.setDirection(request.getDirection());
        log.setEventTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());

        User user = new User();
        user.setId(request.getUserId());
        log.setUser(user);

        Tenant tenant = new Tenant();
        tenant.setId(TenantContext.getCurrentTenant());
        log.setTenant(tenant);

        try {
            AccessLog processed = accessControlService.processAccess(log);
            return ResponseEntity.ok("Access granted and registered.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
