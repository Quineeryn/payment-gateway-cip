package com.payment.payment_gateway.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final HttpStatus status;

    public PaymentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}