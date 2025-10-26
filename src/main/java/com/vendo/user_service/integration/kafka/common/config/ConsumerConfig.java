package com.vendo.user_service.integration.kafka.common.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;

@Configuration
@RequiredArgsConstructor
public class ConsumerConfig {

    private final KafkaProperties kafkaProperties;

    private static final String TRUSTED_PACKAGES = "*";

    private static final String BOOTSTRAP_ADDRESS_TEMPLATE = "%s:%d";

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_ADDRESS_TEMPLATE.formatted(kafkaProperties.getHost(), kafkaProperties.getPort()));

        JsonDeserializer<Object> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages(TRUSTED_PACKAGES);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ?> emailOtpContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ?> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
