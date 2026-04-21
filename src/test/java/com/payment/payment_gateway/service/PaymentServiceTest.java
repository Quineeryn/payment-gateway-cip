package com.payment.payment_gateway.service;

import com.payment.payment_gateway.client.CoreBankingClient;
import com.payment.payment_gateway.dto.request.PaymentRequest;
import com.payment.payment_gateway.dto.response.CoreBankDebitResponse;
import com.payment.payment_gateway.dto.response.BillerPayResponse;
import com.payment.payment_gateway.dto.response.PaymentResponse;
import com.payment.payment_gateway.entity.Transaction;
import com.payment.payment_gateway.enums.Channel;
import com.payment.payment_gateway.enums.TransactionStatus;
import com.payment.payment_gateway.exception.PaymentException;
import com.payment.payment_gateway.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CoreBankingClient coreBankingClient;

    @Mock
    private BillerService billerService;

    @Mock
    private ObjectProvider<KafkaProducerService> kafkaProducerServiceProvider;
    
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("INV-TEST-001");
        paymentRequest.setChannel(Channel.MOBILE_BANKING);
        paymentRequest.setAmount(new BigDecimal("250000"));
        paymentRequest.setAccount("1234567890");
        paymentRequest.setCurrency("IDR");
        paymentRequest.setPaymentMethod("VIRTUAL_ACCOUNT");

        savedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .orderId("INV-TEST-001")
                .channel(Channel.MOBILE_BANKING)
                .amount(new BigDecimal("250000"))
                .account("1234567890")
                .currency("IDR")
                .paymentMethod("VIRTUAL_ACCOUNT")
                .status(TransactionStatus.PENDING)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createPayment_Success() {
        // Arrange
        CoreBankDebitResponse debitResponse = new CoreBankDebitResponse();
        debitResponse.setStatus("SUCCESS");
        debitResponse.setCorebankReference("CB123456");

        BillerPayResponse billerResponse = new BillerPayResponse();
        billerResponse.setStatus("SUCCESS");
        billerResponse.setBillerReference("BILLER123456");

        when(transactionRepository.existsByOrderId(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(coreBankingClient.debit(any())).thenReturn(debitResponse);
        when(billerService.pay(any())).thenReturn(billerResponse);
        
        // Mocking ObjectProvider.ifAvailable
        doAnswer(invocation -> {
            Consumer<KafkaProducerService> consumer = invocation.getArgument(0);
            consumer.accept(kafkaProducerService);
            return null;
        }).when(kafkaProducerServiceProvider).ifAvailable(any(Consumer.class));

        // Act
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
        verify(coreBankingClient).debit(any());
        verify(billerService).pay(any());
        verify(kafkaProducerService).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_DuplicateOrderId_ThrowsException() {
        // Arrange
        when(transactionRepository.existsByOrderId("INV-TEST-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Order ID already exists");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createPayment_InsufficientBalance_ReturnsFailed() {
        // Arrange
        CoreBankDebitResponse debitResponse = new CoreBankDebitResponse();
        debitResponse.setStatus("FAILED");

        when(transactionRepository.existsByOrderId(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(coreBankingClient.debit(any())).thenReturn(debitResponse);

        // Act
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).contains("Insufficient balance");
        verify(billerService, never()).pay(any());
    }

    @Test
    void createPayment_BillerFailed_PerformsReversal() {
        // Arrange
        CoreBankDebitResponse debitResponse = new CoreBankDebitResponse();
        debitResponse.setStatus("SUCCESS");
        debitResponse.setCorebankReference("CB123456");

        BillerPayResponse billerResponse = new BillerPayResponse();
        billerResponse.setStatus("FAILED");

        when(transactionRepository.existsByOrderId(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(coreBankingClient.debit(any())).thenReturn(debitResponse);
        when(billerService.pay(any())).thenReturn(billerResponse);

        // Act
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).contains("refunded");
        verify(coreBankingClient).credit(any());
    }

    @Test
    void createPayment_ExceptionThrown_ReturnsFailed() {
        // Arrange
        when(transactionRepository.existsByOrderId(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(coreBankingClient.debit(any())).thenThrow(new RuntimeException("Connection timeout"));

        // Act
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).contains("processing error");
    }

    @Test
    void getPayment_Success() {
        // Arrange
        UUID id = savedTransaction.getId();
        savedTransaction.setStatus(TransactionStatus.SUCCESS);
        when(transactionRepository.findById(id)).thenReturn(Optional.of(savedTransaction));

        // Act
        PaymentResponse response = paymentService.getPayment(id);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("INV-TEST-001");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void getPayment_NotFound_ThrowsException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPayment(id))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Transaction not found");
    }
}