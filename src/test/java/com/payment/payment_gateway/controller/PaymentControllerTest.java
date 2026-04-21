package com.payment.payment_gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_gateway.dto.request.PaymentRequest;
import com.payment.payment_gateway.dto.response.PaymentResponse;
import com.payment.payment_gateway.enums.Channel;
import com.payment.payment_gateway.enums.TransactionStatus;
import com.payment.payment_gateway.service.PaymentService;
import com.payment.payment_gateway.exception.PaymentException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private PaymentService paymentService;

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void createPayment_Success() throws Exception {
                PaymentRequest request = new PaymentRequest();
                request.setOrderId("INV-TEST-001");
                request.setChannel(Channel.MOBILE_BANKING);
                request.setAmount(new BigDecimal("250000"));
                request.setAccount("1234567890");
                request.setCurrency("IDR");
                request.setPaymentMethod("VIRTUAL_ACCOUNT");

                PaymentResponse mockResponse = PaymentResponse.builder()
                                .transactionId(UUID.randomUUID().toString())
                                .orderId("INV-TEST-001")
                                .status(TransactionStatus.SUCCESS)
                                .corebankReference("CB123456")
                                .billerReference("BILLER123456")
                                .build();

                when(paymentService.createPayment(any())).thenReturn(mockResponse);

                mockMvc.perform(post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.orderId").value("INV-TEST-001"))
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void createPayment_InvalidRequest_ReturnsBadRequest() throws Exception {
                PaymentRequest request = new PaymentRequest();
                // orderId dan account kosong supaya validasi gagal

                mockMvc.perform(post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createPayment_NoAuth_Returns401() throws Exception {
                PaymentRequest request = new PaymentRequest();
                request.setOrderId("INV-TEST-001");
                request.setChannel(Channel.MOBILE_BANKING);
                request.setAmount(new BigDecimal("250000"));
                request.setAccount("1234567890");
                request.setPaymentMethod("VIRTUAL_ACCOUNT");

                mockMvc.perform(post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void getPayment_Success() throws Exception {
                UUID id = UUID.randomUUID();
                PaymentResponse mockResponse = PaymentResponse.builder()
                                .transactionId(id.toString())
                                .orderId("INV-TEST-001")
                                .status(TransactionStatus.SUCCESS)
                                .corebankReference("CB123456")
                                .billerReference("BILLER123456")
                                .build();

                when(paymentService.getPayment(id)).thenReturn(mockResponse);

                mockMvc.perform(get("/api/payments/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value("INV-TEST-001"))
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @WithMockUser(username = "user", roles = "USER")
        void getPayment_NotFound_Returns404() throws Exception {
                UUID id = UUID.randomUUID();
                when(paymentService.getPayment(id))
                                .thenThrow(new PaymentException("Transaction not found", HttpStatus.NOT_FOUND));

                mockMvc.perform(get("/api/payments/" + id))
                                .andExpect(status().isNotFound());
        }
}