package com.payment.payment_gateway.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CoreBankDebitRequest {
    private String account;
    private BigDecimal amount;
}