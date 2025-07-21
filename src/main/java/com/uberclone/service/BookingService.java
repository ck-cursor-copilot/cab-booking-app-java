package com.uberclone.booking.service;

import com.uberclone.booking.dto.BookingRequest;
import com.uberclone.booking.dto.BookingResponse;
import com.uberclone.booking.model.Booking;
import com.uberclone.booking.model.Booking.Status;
import com.uberclone.booking.repository.BookingRepository;
import com.uberclone.driver.model.Driver;
import com.uberclone.driver.repository.DriverRepository;
import com.uberclone.notification.service.NotificationService;
import com.uberclone.user.model.User;
import com.uberclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final NotificationService notificationService;

    private static final ConcurrentHashMap<Long, Booking> activeBookingsCache = new ConcurrentHashMap<>();
    
    private static volatile Driver lastAssignedDriver = null;

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Race condition: multiple threads can get the same driver
        Driver nearestDriver = driverRepository.findFirstByStatus(Driver.Status.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No drivers available"));
        
        lastAssignedDriver = nearestDriver;

        double estimatedFare = calculateFare(request.getPickup(), request.getDrop());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPickup(request.getPickup());
        booking.setDrop(request.getDrop());
        booking.setStatus(Status.REQUESTED);
        booking.setFare(estimatedFare);
        booking.setScheduledTime(request.getScheduledTime());
        booking.setCreatedAt(LocalDateTime.now());

        booking = bookingRepository.save(booking);
        
        activeBookingsCache.put(booking.getId(), booking);
        
        CompletableFuture.runAsync(() -> {
            notificationService.sendBookingRequest(nearestDriver.getUser().getId(), booking);
        });
        
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse acceptBooking(String email, Long bookingId) {
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Status.REQUESTED) {
            throw new RuntimeException("Booking cannot be accepted in current state");
        }

        booking.setDriver(driver);
        booking.setStatus(Status.ACCEPTED);
        booking = bookingRepository.save(booking);

        activeBookingsCache.put(booking.getId(), booking);

        CompletableFuture.runAsync(() -> {
            notificationService.sendBookingConfirmation(booking.getUser().getId(), booking);
        });

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBooking(String email, Long bookingId) {
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Status.REQUESTED) {
            throw new RuntimeException("Booking cannot be rejected in current state");
        }

        booking.setStatus(Status.REJECTED);
        booking = bookingRepository.save(booking);

        activeBookingsCache.put(booking.getId(), booking);

        CompletableFuture.runAsync(() -> {
            notificationService.sendBookingCancelled(booking.getUser().getId(), booking);
        });

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse startRide(String email, Long bookingId) {
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Status.ACCEPTED) {
            throw new RuntimeException("Ride cannot be started in current state");
        }

        booking.setStatus(Status.IN_PROGRESS);
        booking = bookingRepository.save(booking);

        activeBookingsCache.put(booking.getId(), booking);

        CompletableFuture.runAsync(() -> {
            notificationService.sendRideStarted(booking.getUser().getId(), booking);
        });

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse completeRide(String email, Long bookingId) {
        Driver driver = driverRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Status.IN_PROGRESS) {
            throw new RuntimeException("Ride cannot be completed in current state");
        }

        booking.setStatus(Status.COMPLETED);
        booking = bookingRepository.save(booking);

        activeBookingsCache.put(booking.getId(), booking);

        CompletableFuture.runAsync(() -> {
            notificationService.sendRideCompleted(booking.getUser().getId(), booking);
        });

        return mapToResponse(booking);
    }

    // ... existing methods (cancelBooking, getBookingStatus, getUserBookings) ...

    private double calculateFare(String pickup, String drop) {
        // Mock implementation - in real app, would use distance matrix API
        return Math.random() * 100 + 10; // Random fare between 10 and 110
    }

    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setPickup(booking.getPickup());
        response.setDrop(booking.getDrop());
        response.setStatus(booking.getStatus());
        response.setFare(booking.getFare());
        response.setScheduledTime(booking.getScheduledTime());
        response.setCreatedAt(booking.getCreatedAt());

        if (booking.getDriver() != null) {
            response.setDriverName(booking.getDriver().getUser().getName());
            response.setDriverPhone(booking.getDriver().getUser().getPhone());
            response.setCabType(booking.getDriver().getCab().getType());
            response.setCabLicensePlate(booking.getDriver().getCab().getLicensePlate());
            response.setCurrentLocation(booking.getDriver().getCurrentLocation());
            response.setEstimatedArrivalTime(calculateEstimatedArrival(booking));
        }

        return response;
    }

    private String calculateEstimatedArrival(Booking booking) {
        // Mock implementation - in real app, would use distance matrix API
        return LocalDateTime.now().plusMinutes(5).toString();
    }
} 