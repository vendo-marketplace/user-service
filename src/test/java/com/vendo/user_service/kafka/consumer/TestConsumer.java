package com.vendo.user_service.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.integration.kafka.common.dto.PasswordRecoveryEvent;
import com.vendo.user_service.kafka.common.topics.InputTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestConsumer {

    private final ObjectMapper objectMapper;

    private static final LinkedBlockingQueue<String> dataPriorityBlockingList = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = InputTopics.PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC,
            groupId = "test_password_recovery_email_notification_group",
            properties = {"auto.offset.reset=earliest"}
    )
    private void listenPasswordRecoveryEmailNotificationEvent(ConsumerRecord<String, Object> message) throws JsonProcessingException {
        log.info("[PASSWORD_RECOVERY_EMAIL_NOTIFICATION_EVENT_CONSUMER]: Received test message for password recovery: {}", message);
        PasswordRecoveryEvent passwordRecoveryEvent = objectMapper.readValue(message.value().toString(), PasswordRecoveryEvent.class);
        dataPriorityBlockingList.add(passwordRecoveryEvent.token());
    }

    public boolean hasReceived(String value) {
        return dataPriorityBlockingList.contains(value);
    }
}
