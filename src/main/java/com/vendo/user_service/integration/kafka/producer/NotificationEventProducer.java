package com.vendo.user_service.integration.kafka.producer;

import com.vendo.user_service.integration.kafka.common.dto.PasswordRecoveryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.vendo.user_service.integration.kafka.common.topics.OutputTopics.PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendRecoveryPasswordNotificationEvent(PasswordRecoveryEvent passwordRecoveryEvent) {
        log.info("[PASSWORD_RECOVERY_EMAIL_NOTIFICATION_EVENT_PRODUCER]: Sent {} for password recovery email message", PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC);
        kafkaTemplate.send(PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC, passwordRecoveryEvent);
    }

}
