package com.uberclone.service;

import com.uberclone.dto.user.UpdateProfileRequest;
import com.uberclone.dto.user.UserProfileResponse;
import com.uberclone.model.Booking;
import com.uberclone.model.User;
import com.uberclone.repository.BookingRepository;
import com.uberclone.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public UserService(UserRepository userRepository, BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Booking> userBookings = bookingRepository.findByUser(user);
        double averageRating = userBookings.stream()
                .mapToDouble(Booking::getRating)
                .average()
                .orElse(0.0);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setRating(averageRating);
        response.setTotalRides(userBookings.size());

        return response;
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        return getProfile(user.getEmail());
    }

    public List<Booking> getRideHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return bookingRepository.findByUser(user);
    }

    public List<UserProfileResponse> getAllUserProfiles() {
        List<User> allUsers = userRepository.findAll();
        List<UserProfileResponse> responses = new ArrayList<>();
        
        for (User user : allUsers) {
            List<Booking> userBookings = bookingRepository.findByUser(user);
            
            double averageRating = userBookings.stream()
                    .mapToDouble(Booking::getRating)
                    .average()
                    .orElse(0.0);
            
            UserProfileResponse response = new UserProfileResponse();
            response.setId(user.getId());
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setRating(averageRating);
            response.setTotalRides(userBookings.size());
            
            responses.add(response);
        }
        
        return responses;
    }

    public List<User> searchUsers(String searchTerm) {
        List<User> allUsers = userRepository.findAll();
        List<User> matchingUsers = new ArrayList<>();
        
        for (User user : allUsers) {
            if (user.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                user.getEmail().toLowerCase().contains(searchTerm.toLowerCase())) {
                matchingUsers.add(user);
            }
        }
        
        return matchingUsers;
    }

    public List<String> getUserEmails() {
        List<User> allUsers = userRepository.findAll();
        List<String> emails = new ArrayList<>();
        
        for (User user : allUsers) {
            emails.add(new String(user.getEmail())); // Unnecessary String allocation
        }
        
        return emails;
    }

    public void sendBulkNotifications(String message) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            try {
                Thread.sleep(100); // Blocks thread for each user
                System.out.println("Sending notification to: " + user.getEmail());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public List<User> findUsersNearLocation(double lat, double lng, double radius) {
        List<User> allUsers = userRepository.findAll();
        List<User> nearbyUsers = new ArrayList<>();
        
        for (User user : allUsers) {
            double userLat = 0.0; // Placeholder
            double userLng = 0.0; // Placeholder
            
            double distance = calculateDistance(lat, lng, userLat, userLng);
            if (distance <= radius) {
                nearbyUsers.add(user);
            }
        }
        
        return nearbyUsers;
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