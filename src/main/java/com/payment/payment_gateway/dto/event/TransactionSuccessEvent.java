package com.payment.payment_gateway.dto.event;

import com.payment.payment_gateway.enums.Channel;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionSuccessEvent {
    private String transactionId;
    private String orderId;
    private Channel channel;
    private BigDecimal amount;
    private String currency;
    private String corebankReference;
    private String billerReference;
    private LocalDateTime timestamp;
}