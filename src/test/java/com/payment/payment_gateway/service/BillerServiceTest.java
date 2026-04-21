package com.payment.payment_gateway.service;

import com.payment.payment_gateway.client.BillerAggregatorClient;
import com.payment.payment_gateway.dto.request.BillerPayRequest;
import com.payment.payment_gateway.dto.response.BillerPayResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillerServiceTest {

    @Mock
    private BillerAggregatorClient billerAggregatorClient;

    @InjectMocks
    private BillerService billerService;

    @Test
    void pay_Success() {
        // Arrange
        BillerPayRequest request = new BillerPayRequest("INV-001", new BigDecimal("250000"), "VIRTUAL_ACCOUNT");

        BillerPayResponse mockResponse = new BillerPayResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setBillerReference("BILLER123");

        when(billerAggregatorClient.pay(any())).thenReturn(mockResponse);

        // Act
        BillerPayResponse response = billerService.pay(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getBillerReference()).isEqualTo("BILLER123");
    }

    @Test
    void pay_ClientThrowsException_FallbackTriggered() {
        // Arrange
        BillerPayRequest request = new BillerPayRequest("INV-001", new BigDecimal("250000"), "VIRTUAL_ACCOUNT");

        // Act
        BillerPayResponse response = billerService.billerFallback(request, new RuntimeException("Service unavailable"));

        // Assert
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getBillerReference()).isNull();
    }
}