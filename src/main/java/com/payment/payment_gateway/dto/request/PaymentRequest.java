package com.payment.payment_gateway.dto.request;

import com.payment.payment_gateway.enums.Channel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotNull(message = "channel is required")
    private Channel channel;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "account is required")
    private String account;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    private String currency = "IDR";
}