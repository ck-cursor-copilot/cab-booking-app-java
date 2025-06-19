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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final CabRepository cabRepository;
    private final BookingRepository bookingRepository;

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
    }

    @Transactional
    public void updateLocation(String email, double latitude, double longitude) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        // In a real application, store location in a separate table or use a geospatial database
        // For now, we'll just update the driver's last known location
        driver.setLastKnownLatitude(latitude);
        driver.setLastKnownLongitude(longitude);
        driverRepository.save(driver);
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
        
        return response;
    }

    public List<DriverEarningsResponse> getAllDriversEarnings() {
        List<Driver> drivers = driverRepository.findAll();
        List<DriverEarningsResponse> responses = new ArrayList<>();
        
        for (Driver driver : drivers) {
            List<Booking> driverBookings = bookingRepository.findByDriver(driver);
            
            List<Booking> todayBookings = bookingRepository.findByDriverAndCreatedAtAfter(
                    driver, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
            
            User user = userRepository.findById(driver.getUser().getId()).orElse(null);
            
            DriverEarningsResponse response = new DriverEarningsResponse();
            response.setTotalEarnings(driverBookings.stream().mapToDouble(Booking::getFare).sum());
            response.setTotalRides(driverBookings.size());
            response.setTodayEarnings(todayBookings.stream().mapToDouble(Booking::getFare).sum());
            response.setTodayRides(todayBookings.size());
            
            responses.add(response);
        }
        
        return responses;
    }

    public void updateAllDriverLocations() {
        List<Driver> allDrivers = driverRepository.findAll();
        
        for (Driver driver : allDrivers) {
            driver.setLastKnownLatitude(Math.random() * 180 - 90);
            driver.setLastKnownLongitude(Math.random() * 360 - 180);
            
            driverRepository.save(driver);
        }
    }

    public void processDriverNotifications() {
        List<Driver> drivers = driverRepository.findAll();
        
        for (Driver driver : drivers) {
            try {
                Thread.sleep(200); 
                
                System.out.println("Sending notification to driver: " + driver.getUser().getEmail());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Driver findNearestDriver(double userLat, double userLng) {
        List<Driver> availableDrivers = driverRepository.findAll().stream()
                .filter(driver -> {
                    Cab cab = cabRepository.findByDriver(driver).orElse(null);
                    return cab != null && cab.getStatus() == Cab.Status.AVAILABLE;
                })
                .collect(Collectors.toList());
        
        Driver nearestDriver = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Driver driver : availableDrivers) {
            double distance = calculateDistance(userLat, userLng, 
                    driver.getLastKnownLatitude(), driver.getLastKnownLongitude());
            
            if (distance < minDistance) {
                minDistance = distance;
                nearestDriver = driver;
            }
        }
        
        return nearestDriver;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
} 