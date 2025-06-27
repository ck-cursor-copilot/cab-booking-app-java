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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

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

    public void sendBulkNotifications(List<Long> userIds, String message) {
        for (Long userId : userIds) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(NotificationType.GENERAL);
            notification.setTitle("Bulk Notification");
            notification.setMessage(message);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            
            notificationRepository.save(notification);
        }
    }

    public void sendNotificationsWithDelay(List<Long> userIds, String message) {
        for (Long userId : userIds) {
            try {
                Thread.sleep(200); 
                
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setType(NotificationType.GENERAL);
                notification.setTitle("Delayed Notification");
                notification.setMessage(message);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setRead(false);
                
                notificationRepository.save(notification);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public List<Notification> getNotificationsWithDetails(List<Long> userIds) {
        List<Notification> allNotifications = new ArrayList<>();
        
        for (Long userId : userIds) {
            List<Notification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            allNotifications.addAll(userNotifications);
        }
        
        allNotifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));
        
        return allNotifications;
    }

    public void cleanupOldNotifications(int daysOld) {
        List<Notification> allNotifications = notificationRepository.findAll();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        
        List<Notification> oldNotifications = allNotifications.stream()
                .filter(notification -> notification.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());
        
        for (Notification notification : oldNotifications) {
            notificationRepository.delete(notification);
        }
    }

    public Map<String, Object> getNotificationStats() {
        List<Notification> allNotifications = notificationRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        
        long totalNotifications = allNotifications.size();
        long unreadNotifications = allNotifications.stream()
                .filter(notification -> !notification.isRead())
                .count();
        
        Map<NotificationType, Long> notificationsByType = allNotifications.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));
        
        stats.put("totalNotifications", totalNotifications);
        stats.put("unreadNotifications", unreadNotifications);
        stats.put("notificationsByType", notificationsByType);
        
        return stats;
    }
} 