package com.vendo.user_service.integration.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.vendo.user_service.integration.kafka.common.config.OutputTopics.PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendRecoveryPasswordNotificationEvent(String message) {
        log.info("[NOTIFICATION_EVENT_PRODUCER: Sent {} for password recovery email message", PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC);
        kafkaTemplate.send(PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC, message);
    }

}
