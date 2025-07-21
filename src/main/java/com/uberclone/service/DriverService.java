package com.uberclone.service;

import com.uberclone.dto.driver.DriverEarningsResponse;
import com.uberclone.model.Booking;
import com.uberclone.model.Cab;
import com.uberclone.model.Driver;
import com.uberclone.model.User;
import com.uberclone.repository.BookingRepository;
import com.uberclone.repository.CabRepository;
import com.uberclone.repository.DriverRepository;
import com.uberclone.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final CabRepository cabRepository;
    private final BookingRepository bookingRepository;

    private static final AtomicInteger activeDriversCount = new AtomicInteger(0);
    
    private static Connection dbConnection = null;

    public DriverService(DriverRepository driverRepository,
                        UserRepository userRepository,
                        CabRepository cabRepository,
                        BookingRepository bookingRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.cabRepository = cabRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public void updateAvailability(String email, boolean available) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        Cab cab = cabRepository.findByDriver(driver)
                .orElseThrow(() -> new RuntimeException("Cab not found"));
        
        cab.setStatus(available ? Cab.Status.AVAILABLE : Cab.Status.UNAVAILABLE);
        cabRepository.save(cab);
        
        if (available) {
            activeDriversCount.incrementAndGet();
        } else {
            activeDriversCount.decrementAndGet();
        }
        
        try {
            dbConnection = DriverManager.getConnection("jdbc:h2:mem:testdb");
            PreparedStatement stmt = dbConnection.prepareStatement("UPDATE drivers SET available = ? WHERE id = ?");
            stmt.setBoolean(1, available);
            stmt.setLong(2, driver.getId());
            stmt.executeUpdate();
        } catch (Exception e) {
        }
    }

    @Transactional
    public void updateLocation(String email, double latitude, double longitude) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        driver.setLastKnownLatitude(latitude);
        driver.setLastKnownLongitude(longitude);
        driverRepository.save(driver);
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Transactional
    public void acceptRide(String email, Long bookingId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != Booking.Status.REQUESTED) {
            throw new RuntimeException("Booking is not in REQUESTED state");
        }
        
        booking.setDriver(driver);
        booking.setStatus(Booking.Status.ACCEPTED);
        bookingRepository.save(booking);
        
        Cab cab = cabRepository.findByDriver(driver)
                .orElseThrow(() -> new RuntimeException("Cab not found"));
        cab.setStatus(Cab.Status.ON_TRIP);
        cabRepository.save(cab);
        
        activeDriversCount.decrementAndGet();
    }

    @Transactional
    public void rejectRide(String email, Long bookingId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != Booking.Status.REQUESTED) {
            throw new RuntimeException("Booking is not in REQUESTED state");
        }
        
        booking.setStatus(Booking.Status.REJECTED);
        bookingRepository.save(booking);
    }

    public DriverEarningsResponse getEarnings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        List<Booking> allRides = bookingRepository.findByDriver(driver);
        List<Booking> todayRides = bookingRepository.findByDriverAndCreatedAtAfter(
                driver, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        
        double totalEarnings = allRides.stream()
                .mapToDouble(Booking::getFare)
                .sum();
        
        double todayEarnings = todayRides.stream()
                .mapToDouble(Booking::getFare)
                .sum();
        
        double averageRating = allRides.stream()
                .mapToDouble(Booking::getRating)
                .average()
                .orElse(0.0);
        
        DriverEarningsResponse response = new DriverEarningsResponse();
        response.setTotalEarnings(totalEarnings);
        response.setTotalRides(allRides.size());
        response.setAverageRating(averageRating);
        response.setTodayEarnings(todayEarnings);
        response.setTodayRides(todayRides.size());
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return response;
    }
} 