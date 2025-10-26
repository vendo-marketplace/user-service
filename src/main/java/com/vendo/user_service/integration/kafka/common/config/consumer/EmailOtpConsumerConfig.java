package com.vendo.user_service.integration.kafka.common.config.consumer;

import com.vendo.user_service.integration.kafka.common.config.KafkaProperties;
import com.vendo.user_service.integration.kafka.event.EmailOtpEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class EmailOtpConsumerConfig {

    private final KafkaProperties kafkaProperties;

    private static final String BOOTSTRAP_ADDRESS_TEMPLATE = "%s:%d";

    @Bean
    public ConsumerFactory<String, EmailOtpEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_ADDRESS_TEMPLATE.formatted(kafkaProperties.getHost(), kafkaProperties.getPort()));

        JsonDeserializer<EmailOtpEvent> deserializer = new JsonDeserializer<>(EmailOtpEvent.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailOtpEvent> emailOtpContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EmailOtpEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
