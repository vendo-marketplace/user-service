package com.vendo.user_service.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestConsumer {

    private static final LinkedBlockingQueue<String> dataPriorityBlockingList = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = "${kafka.events.password-recovery-email-notification-event.topic}",
            groupId = "${kafka.events.password-recovery-email-notification-event.groupId}",
            properties = {"auto.offset.reset: ${kafka.events.password-recovery-email-notification-event.properties.auto-offset-reset}"}
    )
    private void listenPasswordRecoveryEmailNotificationEvent(@Payload String token) {
        log.info("Received event for password recovery: {}", token);
        dataPriorityBlockingList.add(token);
    }

    public boolean hasReceived(String value) {
        return dataPriorityBlockingList.contains(value);
    }
}
