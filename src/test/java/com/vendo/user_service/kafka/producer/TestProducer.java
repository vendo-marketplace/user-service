package com.vendo.user_service.kafka.producer;

import com.vendo.user_service.integration.kafka.common.topics.OutputTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPasswordRecoveryEmailNotificationEvent(String productId) {
        sendEvent(OutputTopics.PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC, productId);
    }

    private void sendEvent(String topic, Object data) {
        kafkaTemplate.send(topic, data);
    }

}
