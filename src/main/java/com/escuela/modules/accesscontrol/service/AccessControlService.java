package com.escuela.modules.accesscontrol.service;

import com.escuela.modules.accesscontrol.domain.AccessLog;
import com.escuela.modules.accesscontrol.domain.AccessLogRepository;
import com.escuela.modules.notification.service.NotificationService;
import com.escuela.modules.security.domain.User;
import com.escuela.modules.security.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AccessControlService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public AccessLog processAccess(AccessLog log) {
        // Step 5.2: Register log
        AccessLog savedLog = accessLogRepository.save(log);

        // Step 5.4: Orchestrate notification if ENTRADA for Estudiante
        User user = userRepository.findById(log.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (log.getDirection() == AccessLog.Direction.ENTRADA && 
            user.getRole().getName().equals("ESTUDIANTE")) {
            
            notifyParents(user, log);
        }

        return savedLog;
    }

    private void notifyParents(User student, AccessLog log) {
        Set<User> parents = student.getParents();
        String message = String.format("Notificación: Su hijo(a) %s %s ha ingresado a la institución a las %s.",
                student.getFirstName(), student.getLastName(), log.getEventTimestamp());

        for (User parent : parents) {
            // In real app, we'd check if parent has a phone number
            notificationService.sendWhatsApp("PARENT_PHONE", message);
            notificationService.sendEmail(parent.getEmail(), "Ingreso Institucional", message);
        }
    }
}
