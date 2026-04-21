package com.payment.payment_gateway.dto.response;

import lombok.Data;

@Data
public class BillerPayResponse {
    private String billerReference;
    private String status;
}