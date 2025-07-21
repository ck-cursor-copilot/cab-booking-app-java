package com.uberclone.notification.service;

import com.uberclone.notification.model.Notification;
import com.uberclone.notification.model.NotificationType;
import com.uberclone.notification.repository.NotificationRepository;
import com.uberclone.payment.model.Payment;
import com.uberclone.booking.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification sendBookingConfirmation(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.BOOKING_CONFIRMED);
        notification.setTitle("Booking Confirmed");
        notification.setMessage(String.format("Your booking #%d has been confirmed. Driver is on the way!", booking.getId()));
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
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.PAYMENT_CONFIRMED);
        notification.setTitle("Payment Confirmed");
        notification.setMessage(String.format("Payment of $%.2f for booking #%d has been confirmed.",
                payment.getAmount(), payment.getBooking().getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
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
    public Notification sendRideStarted(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.RIDE_STARTED);
        notification.setTitle("Ride Started");
        notification.setMessage(String.format("Your ride for booking #%d has started. Enjoy your journey!", booking.getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendRideCompleted(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.RIDE_COMPLETED);
        notification.setTitle("Ride Completed");
        notification.setMessage(String.format("Your ride for booking #%d has been completed. Please rate your experience.", booking.getId()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendBookingRequest(Long userId, Booking booking) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.BOOKING_REQUEST);
        notification.setTitle("New Booking Request");
        notification.setMessage(String.format("You have a new booking request #%d from %s to %s", 
                booking.getId(), booking.getPickup(), booking.getDrop()));
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