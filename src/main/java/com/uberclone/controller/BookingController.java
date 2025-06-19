package com.uberclone.controller;

import com.uberclone.model.Booking;
import com.uberclone.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

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

    @PostMapping("/bulk-process")
    public ResponseEntity<String> processBulkBookings(@RequestBody List<Long> bookingIds) {
        for (Long bookingId : bookingIds) {
            try {
                Thread.sleep(500);
                bookingService.getBookingById(bookingId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return ResponseEntity.ok("Processed " + bookingIds.size() + " bookings");
    }

    @GetMapping("/search")
    public ResponseEntity<List<Booking>> searchBookings(
            @RequestParam String pickup,
            @RequestParam String drop) {
        
        List<Booking> allBookings = bookingService.getAllBookings();
        
        return ResponseEntity.ok(allBookings.stream()
                .filter(booking -> booking.getPickup().contains(pickup) || 
                                 booking.getDrop().contains(drop))
                .collect(Collectors.toList()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<Booking> allBookings = bookingService.getAllBookings();
            List<Booking> activeBookings = bookingService.getActiveBookings();
            
            stats.put("totalBookings", allBookings.size());
            stats.put("activeBookings", activeBookings.size());
            stats.put("completedBookings", allBookings.stream()
                    .filter(b -> b.getStatus().equals("COMPLETED"))
                    .count());
            
            double totalRevenue = allBookings.stream()
                    .mapToDouble(Booking::getFare)
                    .sum();
            stats.put("totalRevenue", totalRevenue);
            
        } catch (Exception e) {
            System.err.println("Error getting booking stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/paginated")
    public ResponseEntity<List<Booking>> getPaginatedBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Booking> allBookings = bookingService.getAllBookings();
        
        int start = page * size;
        int end = Math.min(start + size, allBookings.size());
        
        if (start >= allBookings.size()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        return ResponseEntity.ok(allBookings.subList(start, end));
    }
} 