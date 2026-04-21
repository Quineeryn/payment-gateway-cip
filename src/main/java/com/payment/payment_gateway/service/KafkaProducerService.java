package com.payment.payment_gateway.service;

import com.payment.payment_gateway.config.KafkaConfig;
import com.payment.payment_gateway.dto.event.TransactionSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaProducerService {

    private final KafkaTemplate<String, TransactionSuccessEvent> kafkaTemplate;

    public void publishTransactionSuccess(TransactionSuccessEvent event) {
        log.info("Publishing transaction success event for orderId: {}", event.getOrderId());
        
        kafkaTemplate.send(KafkaConfig.TOPIC_TRANSACTION_SUCCESS, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for orderId: {}", event.getOrderId(), ex);
                    } else {
                        log.info("Event published successfully for orderId: {}", event.getOrderId());
                    }
                });
    }
}