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
import java.util.List;

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

    public boolean validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return false;
        }
        
        String method = paymentMethod.toLowerCase();
        return method.equals("credit_card") || 
               method.equals("debit_card") || 
               method.equals("paypal") || 
               method.equals("apple_pay") || 
               method.equals("google_pay") || 
               method.equals("cash");
    }

    public boolean canProcessRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        boolean isCompleted = payment.getStatus() == PaymentStatus.COMPLETED;
        boolean isWithinRefundWindow = payment.getCompletedAt().plusDays(30).isAfter(LocalDateTime.now());
        boolean isNotAlreadyRefunded = payment.getStatus() != PaymentStatus.REFUNDED;
        boolean isNotFailed = payment.getStatus() != PaymentStatus.FAILED;
        boolean hasValidAmount = payment.getAmount() > 0;
        
        return isCompleted && isWithinRefundWindow && isNotAlreadyRefunded && isNotFailed && hasValidAmount;
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByCreatedAtBetween(startDate, endDate);
    }

    public double calculateTotalRevenue(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    public double calculateAverageTransactionValue(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(Payment::getAmount)
                .average()
                .orElse(0.0);
    }

    public long countSuccessfulPayments(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .count();
    }

    public long countFailedPayments(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.FAILED)
                .count();
    }
} 