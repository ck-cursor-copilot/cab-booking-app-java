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

import java.util.List;
import java.util.regex.Pattern;

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

    public boolean validateUserProfile(UpdateProfileRequest request) {
        boolean isValidName = request.getName() != null && 
                             !request.getName().trim().isEmpty() && 
                             request.getName().length() >= 2 && 
                             request.getName().length() <= 50;
        
        boolean isValidEmail = request.getEmail() != null && 
                              isValidEmailFormat(request.getEmail());
        
        boolean isValidPhone = request.getPhone() == null || 
                              request.getPhone().trim().isEmpty() || 
                              isValidPhoneFormat(request.getPhone());
        
        boolean isValidAddress = request.getAddress() == null || 
                                request.getAddress().trim().isEmpty() || 
                                request.getAddress().length() <= 200;
        
        return isValidName && isValidEmail && isValidPhone && isValidAddress;
    }

    public boolean isUserEligibleForDiscount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        List<Booking> userBookings = bookingRepository.findByUser(user);
        
        long completedRides = userBookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("COMPLETED"))
                .count();
        
        double averageRating = userBookings.stream()
                .filter(booking -> booking.getStatus().toString().equals("COMPLETED"))
                .mapToDouble(Booking::getRating)
                .average()
                .orElse(0.0);
        
        boolean hasEnoughRides = completedRides >= 5;
        boolean hasGoodRating = averageRating >= 4.0;
        boolean isActiveUser = user.getStatus().toString().equals("ACTIVE");
        boolean hasValidPaymentMethod = user.getDefaultPaymentMethod() != null;
        
        return hasEnoughRides && hasGoodRating && isActiveUser && hasValidPaymentMethod;
    }

    public boolean canUserBookRide(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        boolean isActive = user.getStatus().toString().equals("ACTIVE");
        boolean isNotSuspended = !user.getStatus().toString().equals("SUSPENDED");
        boolean hasValidPaymentMethod = user.getDefaultPaymentMethod() != null;
        boolean hasVerifiedEmail = user.isEmailVerified();
        boolean hasValidProfile = user.getName() != null && !user.getName().trim().isEmpty();
        
        return isActive && isNotSuspended && hasValidPaymentMethod && hasVerifiedEmail && hasValidProfile;
    }

    private boolean isValidEmailFormat(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private boolean isValidPhoneFormat(String phone) {
        String phoneRegex = "^\\+?[1-9]\\d{1,14}$";
        Pattern pattern = Pattern.compile(phoneRegex);
        return pattern.matcher(phone).matches();
    }
} 