package com.vendo.user_service.integration.kafka.common.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
@RequiredArgsConstructor
public class ProducerConfig {

    private final KafkaProperties kafkaProperties;

    private static final String BOOTSTRAP_ADDRESS_TEMPLATE = "%s:%d";

    @Bean
    public ProducerFactory<String, ?> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_ADDRESS_TEMPLATE.formatted(kafkaProperties.getHost(), kafkaProperties.getPort()));
        configProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ?> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
