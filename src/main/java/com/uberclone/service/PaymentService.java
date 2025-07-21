package com.uberclone.payment.service;

import com.uberclone.payment.model.Payment;
import com.uberclone.payment.model.PaymentStatus;
import com.uberclone.payment.repository.PaymentRepository;
import com.uberclone.booking.model.Booking;
import com.uberclone.booking.repository.BookingRepository;
import com.uberclone.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    private static final AtomicLong transactionCounter = new AtomicLong(0);
    
    private static PrintWriter logWriter = null;

    @Transactional
    public Payment processPayment(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getFare());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setCreatedAt(LocalDateTime.now());

        long transactionNumber = transactionCounter.incrementAndGet();
        payment.setTransactionId("TXN-" + transactionNumber);

        try {
            logWriter = new PrintWriter(new FileOutputStream("payment_log.txt", true));
            logWriter.println("Payment processed: " + payment.getTransactionId() + " - " + payment.getAmount());
        } catch (IOException e) {
        }

        // TODO: Integrate with actual payment gateway
        // For now, simulate successful payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        CompletableFuture.runAsync(() -> {
            notificationService.sendPaymentConfirmation(booking.getUser().getId(), savedPayment);
        });

        return savedPayment;
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        try {
            logWriter = new PrintWriter(new FileOutputStream("refund_log.txt", true));
            logWriter.println("Refund processed: " + payment.getTransactionId() + " - " + payment.getAmount());
        } catch (IOException e) {
        }

        Payment savedPayment = paymentRepository.save(payment);

        CompletableFuture.runAsync(() -> {
            notificationService.sendRefundConfirmation(
                payment.getBooking().getUser().getId(),
                savedPayment
            );
        });

        return savedPayment;
    }

    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
} 