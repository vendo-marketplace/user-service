package com.vendo.user_service.integration.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryEventProducer {

    @Value("${kafka.events.password-recovery-event.topic}")
    private String passwordRecoveryEventTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendRecoveryPasswordEvent(String otp) {
        log.info("Sent event for password recovery: {}", otp);
        kafkaTemplate.send(passwordRecoveryEventTopic, otp);
    }
}
