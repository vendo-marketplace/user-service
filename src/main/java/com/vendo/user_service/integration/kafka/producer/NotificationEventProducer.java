package com.vendo.user_service.integration.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;

    public void sendRecoveryPasswordNotificationEvent(PasswordRecoveryEvent passwordRecoveryEvent) {
        log.info("[PASSWORD_RECOVERY_EMAIL_NOTIFICATION_EVENT_PRODUCER]: Sent {} for password recovery email message", PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC);
        try {
            kafkaTemplate.send(PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC, objectMapper.writeValueAsString(passwordRecoveryEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
