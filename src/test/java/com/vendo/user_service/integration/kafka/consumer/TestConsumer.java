package com.vendo.user_service.integration.kafka.consumer;

import com.vendo.user_service.integration.kafka.event.EmailOtpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestConsumer {

    private static final LinkedBlockingQueue<String> dataPriorityBlockingList = new LinkedBlockingQueue<>();

    @KafkaListener(
            topics = "${kafka.events.email-otp-notification-event.topic}",
            groupId = "${kafka.events.email-otp-notification-event.groupId}",
            properties = {"auto.offset.reset: ${kafka.events.email-otp-notification-event.properties.auto-offset-reset}"},
            containerFactory = "emailOtpContainerFactory"
    )
    private void listenEmailVerificationEvent(EmailOtpEvent event) {
        log.info("Received event for email otp notification: {}", event);
        dataPriorityBlockingList.add(event.getEmail());
    }

    public boolean removeIfReceived(String value) {
        return dataPriorityBlockingList.remove(value);
    }
}
