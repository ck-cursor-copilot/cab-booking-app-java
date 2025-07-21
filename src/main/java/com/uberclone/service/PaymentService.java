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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Transactional
    public Payment processPayment(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(Math.abs(booking.getFare()));
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCompletedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

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
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
} 