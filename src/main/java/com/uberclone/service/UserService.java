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

        boolean x = !user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail());
        if (x) {
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
} 