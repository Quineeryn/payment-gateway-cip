package com.payment.payment_gateway.dto.response;

import lombok.Data;

@Data
public class CoreBankDebitResponse {
    private String corebankReference;
    private String status;
}