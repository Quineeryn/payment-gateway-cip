package com.payment.payment_gateway.controller;

import com.payment.payment_gateway.dto.request.BillerPayRequest;
import com.payment.payment_gateway.dto.request.CoreBankDebitRequest;
import com.payment.payment_gateway.dto.response.BillerPayResponse;
import com.payment.payment_gateway.dto.response.CoreBankDebitResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Slf4j
public class MockExternalController {

    @PostMapping("/api/corebank/debit")
    public CoreBankDebitResponse mockDebit(@RequestBody CoreBankDebitRequest request) {
        log.info("Mock Core Banking debit: account={}, amount={}", request.getAccount(), request.getAmount());
        CoreBankDebitResponse response = new CoreBankDebitResponse();
        
        // Skenario: Saldo tidak cukup jika jumlah > 1.000.000
        if (request.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            response.setStatus("FAILED");
            log.warn("Mock Core Banking: Insufficient balance for account {}", request.getAccount());
            return response;
        }

        response.setCorebankReference(generateRef("CB"));
        response.setStatus("SUCCESS");
        return response;
    }

    @PostMapping("/api/corebank/credit")
    public CoreBankDebitResponse mockCredit(@RequestBody CoreBankDebitRequest request) {
        log.info("Mock Core Banking credit (Refund): account={}, amount={}", request.getAccount(), request.getAmount());
        CoreBankDebitResponse response = new CoreBankDebitResponse();
        response.setCorebankReference(generateRef("RF"));
        response.setStatus("SUCCESS");
        return response;
    }

    @PostMapping("/api/biller/pay")
    public BillerPayResponse mockBillerPay(@RequestBody BillerPayRequest request) {
        log.info("Mock Biller pay: orderId={}, amount={}", request.getOrderId(), request.getAmount());
        BillerPayResponse response = new BillerPayResponse();
        
        // Skenario: Biller gagal jika paymentMethod adalah "FAIL"
        if ("FAIL".equals(request.getPaymentMethod())) {
            response.setStatus("FAILED");
            log.warn("Mock Biller: Payment failed for order {}", request.getOrderId());
            return response;
        }

        response.setBillerReference(generateRef("BILLER"));
        response.setStatus("SUCCESS");
        return response;
    }

    private String generateRef(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}