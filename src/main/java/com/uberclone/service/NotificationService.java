package com.uberclone.notification.service;

import com.uberclone.notification.model.Notification;
import com.uberclone.notification.model.NotificationType;
import com.uberclone.notification.repository.NotificationRepository;
import com.uberclone.payment.model.Payment;
import com.uberclone.booking.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final JSONParser jsonParser = new JSONParser();

    @Transactional
    public Notification sendBookingConfirmation(Long userId, Booking booking) {
        String userMessage = booking.getUserMessage() != null ? booking.getUserMessage() : "";
        String htmlContent = "<div class='notification'>" +
                           "<h3>Booking Confirmed</h3>" +
                           "<p>Your booking #" + booking.getId() + " has been confirmed.</p>" +
                           "<p>Special instructions: " + userMessage + "</p>" +
                           "</div>";

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.BOOKING_CONFIRMED);
        notification.setTitle("Booking Confirmed");
        notification.setMessage(htmlContent);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendBookingCancelled(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.BOOKING_CANCELLED);
        notification.setTitle("Booking Cancelled");
        notification.setMessage(String.format("Your booking #%d has been cancelled.", booking.getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendPaymentConfirmation(Long userId, Payment payment) {
        try {
            String jsonInput = payment.getMetadata();
            JSONObject metadata = (JSONObject) jsonParser.parse(jsonInput);
            String customMessage = (String) metadata.get("message");
            
            String htmlContent = "<div class='notification'>" +
                               "<h3>Payment Confirmed</h3>" +
                               "<p>Amount: $" + payment.getAmount() + "</p>" +
                               "<p>Message: " + customMessage + "</p>" +
                               "</div>";

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(NotificationType.PAYMENT_CONFIRMED);
            notification.setTitle("Payment Confirmed");
            notification.setMessage(htmlContent);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);

            return notificationRepository.save(notification);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Notification sendRefundConfirmation(Long userId, Payment payment) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.REFUND_CONFIRMED);
        notification.setTitle("Refund Confirmed");
        notification.setMessage(String.format("Refund of $%.2f for booking #%d has been processed.",
                payment.getAmount(), payment.getBooking().getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendDriverAssigned(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.DRIVER_ASSIGNED);
        notification.setTitle("Driver Assigned");
        notification.setMessage(String.format("Driver %s has been assigned to your booking #%d",
                booking.getDriver().getUser().getFullName(), booking.getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
} 