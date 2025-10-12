package com.vendo.user_service.integration.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    @Value("${kafka.events.password-recovery-email-notification-event.topic}")
    private String passwordRecoveryEmailNotificationEventTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendRecoveryPasswordNotificationEvent(String token) {
        log.info("Sent event for password recovery: {}", token);
        kafkaTemplate.send(passwordRecoveryEmailNotificationEventTopic, token);
    }
}
