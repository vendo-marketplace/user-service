package com.vendo.user_service.kafka.consumer;

import com.vendo.user_service.kafka.common.topics.InputTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
public class TestConsumer {

    private static final LinkedBlockingQueue<String> dataPriorityBlockingList = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = InputTopics.PASSWORD_RECOVERY_EMAIL_NOTIFICATION_TOPIC,
            groupId = "test_password_recovery_email_notification_group",
            properties = {"auto.offset.reset=latest"}
    )
    private void listenPasswordRecoveryEmailNotificationEvent(String token) {
        dataPriorityBlockingList.add(token);
    }

    public boolean hasReceived(String value) {
        return dataPriorityBlockingList.contains(value);
    }
}
