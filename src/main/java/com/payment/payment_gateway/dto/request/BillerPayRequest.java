package com.payment.payment_gateway.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BillerPayRequest {
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
}