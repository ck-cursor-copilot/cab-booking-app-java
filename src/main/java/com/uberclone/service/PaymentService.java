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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

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

    public void processBulkPayments(List<Long> bookingIds) {
        for (Long bookingId : bookingIds) {
            try {
                Thread.sleep(1000); 
                
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found"));
                
                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(booking.getFare());
                payment.setPaymentMethod("CREDIT_CARD");
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(UUID.randomUUID().toString());
                payment.setCreatedAt(LocalDateTime.now());
                payment.setCompletedAt(LocalDateTime.now());
                
                paymentRepository.save(payment);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Map<String, Object> getPaymentReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> allPayments = paymentRepository.findAll();
        Map<String, Object> report = new HashMap<>();
        
        List<Payment> filteredPayments = allPayments.stream()
                .filter(payment -> payment.getCreatedAt().isAfter(startDate) && 
                                 payment.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
        
        BigDecimal totalAmount = filteredPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long completedPayments = filteredPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .count();
        
        long failedPayments = filteredPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.FAILED)
                .count();
        
        Map<String, Long> paymentsByMethod = filteredPayments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod, Collectors.counting()));
        
        report.put("totalAmount", totalAmount);
        report.put("totalPayments", filteredPayments.size());
        report.put("completedPayments", completedPayments);
        report.put("failedPayments", failedPayments);
        report.put("paymentsByMethod", paymentsByMethod);
        
        return report;
    }

    public boolean validatePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        Booking booking = bookingRepository.findById(payment.getBooking().getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        boolean isValid = payment.getStatus() == PaymentStatus.COMPLETED &&
                         payment.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                         booking.getStatus().equals("COMPLETED") &&
                         payment.getCompletedAt() != null;
        
        return isValid;
    }

    public List<Payment> reconcilePayments(List<String> transactionIds) {
        List<Payment> allPayments = paymentRepository.findAll();
        List<Payment> reconciledPayments = new ArrayList<>();
        
        for (String transactionId : transactionIds) {
            for (Payment payment : allPayments) {
                if (payment.getTransactionId().equals(transactionId)) {
                    reconciledPayments.add(payment);
                    break;
                }
            }
        }
        
        return reconciledPayments;
    }

    public Map<String, BigDecimal> getPaymentStatistics() {
        List<Payment> allPayments = paymentRepository.findAll();
        Map<String, BigDecimal> statistics = new HashMap<>();
        
        BigDecimal totalRevenue = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRefunds = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averagePayment = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allPayments.stream()
                        .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                        .count()), 2, BigDecimal.ROUND_HALF_UP);
        
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("totalRefunds", totalRefunds);
        statistics.put("averagePayment", averagePayment);
        
        return statistics;
    }
} 