package com.payment.payment_gateway.client;

import com.payment.payment_gateway.dto.request.CoreBankDebitRequest;
import com.payment.payment_gateway.dto.response.CoreBankDebitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "corebank-service", url = "${corebank.service.url}")
public interface CoreBankingClient {

    @PostMapping("/api/corebank/debit")
    CoreBankDebitResponse debit(@RequestBody CoreBankDebitRequest request);

    @PostMapping("/api/corebank/credit")
    CoreBankDebitResponse credit(@RequestBody CoreBankDebitRequest request);
}