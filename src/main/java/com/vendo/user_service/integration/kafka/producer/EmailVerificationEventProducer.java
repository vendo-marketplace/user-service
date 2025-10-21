package com.vendo.user_service.integration.kafka.producer;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationEventProducer {

    @Value("${kafka.events.email-verification-event.topic}")
    private String emailVerificationEventTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEmailVerificationEvent(String otp) {
        log.info("Sent event for email verification: {}", otp);
        kafkaTemplate.send(emailVerificationEventTopic, otp);
    }
}
