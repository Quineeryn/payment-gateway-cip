package com.payment.payment_gateway.service;

import com.payment.payment_gateway.client.CoreBankingClient;
import com.payment.payment_gateway.dto.event.TransactionSuccessEvent;
import com.payment.payment_gateway.dto.request.*;
import com.payment.payment_gateway.dto.response.*;
import com.payment.payment_gateway.entity.Transaction;
import com.payment.payment_gateway.enums.TransactionStatus;
import com.payment.payment_gateway.exception.PaymentException;
import com.payment.payment_gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final CoreBankingClient coreBankingClient;
    private final BillerService billerService;
    private final ObjectProvider<KafkaProducerService> kafkaProducerServiceProvider;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Processing payment for orderId: {}", request.getOrderId());

        if (transactionRepository.existsByOrderId(request.getOrderId())) {
            throw new PaymentException("Order ID already exists", HttpStatus.CONFLICT);
        }

        Transaction transaction = Transaction.builder()
                .orderId(request.getOrderId())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .account(request.getAccount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Step 1: Debit Core Banking
            CoreBankDebitResponse debitResponse = coreBankingClient.debit(
                    new CoreBankDebitRequest(request.getAccount(), request.getAmount()));

            if (!"SUCCESS".equals(debitResponse.getStatus())) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                return buildFailedResponse(transaction, "Insufficient balance or invalid account");
            }

            transaction.setCorebankReference(debitResponse.getCorebankReference());

            // Step 2: Forward ke Biller
            BillerPayResponse billerResponse = billerService.pay(
                    new BillerPayRequest(request.getOrderId(), request.getAmount(), request.getPaymentMethod()));

            if (!"SUCCESS".equals(billerResponse.getStatus())) {
                log.warn("Biller failure for order {}. Performing compensation (Refund).", request.getOrderId());
                performReversal(request.getAccount(), request.getAmount(), transaction);
                return buildFailedResponse(transaction, "Biller payment failed, amount has been refunded");
            }

            transaction.setBillerReference(billerResponse.getBillerReference());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            // Step 3: Kafka Notification (Optional)
            publishEvent(transaction);

            return PaymentResponse.builder()
                    .transactionId(transaction.getId().toString())
                    .orderId(transaction.getOrderId())
                    .status(transaction.getStatus())
                    .corebankReference(transaction.getCorebankReference())
                    .billerReference(transaction.getBillerReference())
                    .build();

        } catch (Exception e) {
            log.error("Fatal payment error for {}: {}", request.getOrderId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            return buildFailedResponse(transaction, "Internal processing error: " + e.getMessage());
        }
    }

    private void performReversal(String account, java.math.BigDecimal amount, Transaction transaction) {
        try {
            coreBankingClient.credit(new CoreBankDebitRequest(account, amount));
            transaction.setStatus(TransactionStatus.FAILED);
            log.info("Reversal success for orderId: {}", transaction.getOrderId());
        } catch (Exception e) {
            log.error("CRITICAL: Reversal failed for orderId: {}. Manual action required.", transaction.getOrderId());
            transaction.setStatus(TransactionStatus.FAILED);
        }
        transactionRepository.save(transaction);
    }

    private void publishEvent(Transaction transaction) {
        kafkaProducerServiceProvider.ifAvailable(service -> {
            try {
                TransactionSuccessEvent event = TransactionSuccessEvent.builder()
                        .transactionId(transaction.getId().toString())
                        .orderId(transaction.getOrderId())
                        .amount(transaction.getAmount())
                        .timestamp(LocalDateTime.now())
                        .build();
                service.publishTransactionSuccess(event);
            } catch (Exception e) {
                log.warn("Kafka event publishing failed: {}", e.getMessage());
            }
        });
    }

    public PaymentResponse getPayment(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Transaction not found", HttpStatus.NOT_FOUND));

        return PaymentResponse.builder()
                .transactionId(transaction.getId().toString())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .corebankReference(transaction.getCorebankReference())
                .billerReference(transaction.getBillerReference())
                .build();
    }

    private PaymentResponse buildFailedResponse(Transaction transaction, String message) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId().toString())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .message(message)
                .build();
    }
}