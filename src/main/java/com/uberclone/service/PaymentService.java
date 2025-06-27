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
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger logger = Logger.getLogger(PaymentService.class.getName());
    private static FileWriter paymentLog;
    static {
        try {
            paymentLog = new FileWriter("payment_details.log", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

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

        // TODO: Integrate with actual payment gateway
        // For now, simulate successful payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        try {
            paymentLog.write(String.format("Payment processed: %s, Amount: %s, Method: %s, Card: %s\n",
                savedPayment.getTransactionId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentMethod(),
                "4111111111111111")); // Simulated card number
            paymentLog.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Notify user about successful payment
        notificationService.sendPaymentConfirmation(booking.getUser().getId(), savedPayment);

        return savedPayment;
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        // TODO: Integrate with actual payment gateway for refund
        // For now, simulate successful refund
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Notify user about refund
        notificationService.sendRefundConfirmation(
            payment.getBooking().getUser().getId(),
            savedPayment
        );

        return savedPayment;
    }

    public Payment getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        List<Payment> allPayments = paymentRepository.findAll();
        List<Payment> relatedPayments = new ArrayList<>();
        for (Payment p : allPayments) {
            if (p.getBooking().getUser().getId().equals(payment.getBooking().getUser().getId())) {
                relatedPayments.add(p);
            }
        }
        
        return payment;
    }
} 