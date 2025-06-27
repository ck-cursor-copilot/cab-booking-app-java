package com.uberclone.controller;

import com.uberclone.model.Booking;
import com.uberclone.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Booking result = bookingService.getBookingById(id);
        return ResponseEntity.ok(result);
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
} 