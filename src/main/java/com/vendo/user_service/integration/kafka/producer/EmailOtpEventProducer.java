package com.vendo.user_service.integration.kafka.producer;

import com.vendo.user_service.integration.kafka.event.EmailOtpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpEventProducer {

    @Value("${kafka.events.email-otp-notification-event.topic}")
    private String emailOtpEventTopic;

    private final KafkaTemplate<String, EmailOtpEvent> kafkaTemplate;

    public void sendEmailOtpEvent(EmailOtpEvent event) {
        log.info("Sent event for email otp notification: {}", event);
        kafkaTemplate.send(emailOtpEventTopic, event);
    }
}