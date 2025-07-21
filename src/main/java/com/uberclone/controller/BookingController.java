package com.uberclone.controller;

import com.uberclone.model.Booking;
import com.uberclone.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.createBooking(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Booking>> getDriverBookings(@PathVariable Long driverId) {
        return ResponseEntity.ok(bookingService.getBookingsByDriverId(driverId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(
            @PathVariable Long id,
            @RequestBody Booking booking) {
        return ResponseEntity.ok(bookingService.updateBooking(id, booking));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Booking>> getActiveBookings() {
        return ResponseEntity.ok(bookingService.getActiveBookings());
    }

    @GetMapping("/history")
    public ResponseEntity<List<Booking>> getBookingHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(bookingService.getBookingHistory(userId, status));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Booking>> filterBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Double minFare,
            @RequestParam(required = false) Double maxFare,
            @RequestParam(required = false) String pickupLocation,
            @RequestParam(required = false) String dropLocation) {
        
        List<Booking> allBookings = bookingService.getAllBookings();
        
        List<Booking> filteredBookings = allBookings.stream()
                .filter(booking -> status == null || booking.getStatus().toString().equals(status))
                .filter(booking -> dateFrom == null || booking.getCreatedAt().toString().compareTo(dateFrom) >= 0)
                .filter(booking -> dateTo == null || booking.getCreatedAt().toString().compareTo(dateTo) <= 0)
                .filter(booking -> minFare == null || booking.getFare() >= minFare)
                .filter(booking -> maxFare == null || booking.getFare() <= maxFare)
                .filter(booking -> pickupLocation == null || booking.getPickup().contains(pickupLocation))
                .filter(booking -> dropLocation == null || booking.getDrop().contains(dropLocation))
                .toList();
        
        return ResponseEntity.ok(filteredBookings);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Object> getBookingStatistics(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long userId) {
        
        List<Booking> bookings = userId != null ? 
                bookingService.getBookingsByUserId(userId) : 
                bookingService.getAllBookings();
        
        long totalBookings = bookings.size();
        long completedBookings = bookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("COMPLETED"))
                .count();
        long cancelledBookings = bookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("CANCELLED"))
                .count();
        double totalRevenue = bookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("COMPLETED"))
                .mapToDouble(Booking::getFare)
                .sum();
        double averageFare = bookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("COMPLETED"))
                .mapToDouble(Booking::getFare)
                .average()
                .orElse(0.0);
        
        return ResponseEntity.ok(Map.of(
                "totalBookings", totalBookings,
                "completedBookings", completedBookings,
                "cancelledBookings", cancelledBookings,
                "totalRevenue", totalRevenue,
                "averageFare", averageFare
        ));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Booking> rateBooking(
            @PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {
        
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }
        
        Booking booking = bookingService.getBookingById(id);
        booking.setRating(rating);
        if (comment != null && !comment.trim().isEmpty()) {
            booking.setComment(comment);
        }
        
        return ResponseEntity.ok(bookingService.updateBooking(id, booking));
    }
} 