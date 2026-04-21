package com.payment.payment_gateway.dto.response;

import com.payment.payment_gateway.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String transactionId;
    private String orderId;
    private TransactionStatus status;
    private String corebankReference;
    private String billerReference;
    private String message;
}