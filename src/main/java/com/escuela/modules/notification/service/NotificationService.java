package com.escuela.modules.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendEmail(String to, String subject, String body) {
        // Step 5.3: SMTP Integration (Conceptual)
        logger.info("Sending Email to {}: {} - {}", to, subject, body);
    }

    public void sendWhatsApp(String phoneNumber, String message) {
        // Step 5.3: WhatsApp Business API Integration (Conceptual)
        logger.info("Sending WhatsApp to {}: {}", phoneNumber, message);
    }
}
