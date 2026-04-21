package com.payment.payment_gateway.client;

import com.payment.payment_gateway.dto.request.BillerPayRequest;
import com.payment.payment_gateway.dto.response.BillerPayResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "biller-service", url = "${biller.service.url}")
public interface BillerAggregatorClient {

    @PostMapping("/api/biller/pay")
    BillerPayResponse pay(@RequestBody BillerPayRequest request);
}