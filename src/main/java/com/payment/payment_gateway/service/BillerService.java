package com.payment.payment_gateway.service;

import com.payment.payment_gateway.client.BillerAggregatorClient;
import com.payment.payment_gateway.dto.request.BillerPayRequest;
import com.payment.payment_gateway.dto.response.BillerPayResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillerService {

    private final BillerAggregatorClient billerAggregatorClient;

    @CircuitBreaker(name = "billerService", fallbackMethod = "billerFallback")
    @Retry(name = "billerService")
    public BillerPayResponse pay(BillerPayRequest request) {
        log.info("Calling Biller Aggregator for orderId: {}", request.getOrderId());
        return billerAggregatorClient.pay(request);
    }

    public BillerPayResponse billerFallback(BillerPayRequest request, Exception ex) {
        log.error("Biller fallback triggered for orderId: {}, reason: {}",
                request.getOrderId(), ex.getMessage());
        BillerPayResponse fallbackResponse = new BillerPayResponse();
        fallbackResponse.setStatus("FAILED");
        fallbackResponse.setBillerReference(null);
        return fallbackResponse;
    }
}