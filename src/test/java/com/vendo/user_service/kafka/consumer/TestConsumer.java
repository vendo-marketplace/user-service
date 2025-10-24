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
            topics = "${kafka.events.password-recovery-event.topic}",
            groupId = "${kafka.events.password-recovery-event.groupId}",
            properties = {"auto.offset.reset: ${kafka.events.password-recovery-event.properties.auto-offset-reset}"}
    )
    private void listenPasswordRecoveryEvent(@Payload String email) {
        log.info("Received event for password recovery: {}", email);
        dataPriorityBlockingList.add(email);
    }

    @KafkaListener(
            topics = "${kafka.events.email-verification-event.topic}",
            groupId = "${kafka.events.email-verification-event.groupId}",
            properties = {"auto.offset.reset: ${kafka.events.email-verification-event.properties.auto-offset-reset}"}
    )
    private void listenEmailVerificationEvent(@Payload String email) {
        log.info("Received event for email verification: {}", email);
        dataPriorityBlockingList.add(email);
    }

    public boolean removeIfReceived(String value) {
        return dataPriorityBlockingList.remove(value);
    }
}
