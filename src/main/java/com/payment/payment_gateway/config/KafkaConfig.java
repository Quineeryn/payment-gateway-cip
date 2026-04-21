package com.payment.payment_gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    public static final String TOPIC_TRANSACTION_SUCCESS = "transaction.success";

    @Bean
    public NewTopic transactionSuccessTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTION_SUCCESS)
                .partitions(1)
                .replicas(1)
                .build();
    }
}