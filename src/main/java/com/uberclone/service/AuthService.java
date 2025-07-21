package com.uberclone.service;

import com.uberclone.dto.auth.AuthResponse;
import com.uberclone.dto.auth.LoginRequest;
import com.uberclone.dto.auth.RegisterRequest;
import com.uberclone.model.User;
import com.uberclone.repository.UserRepository;
import com.uberclone.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    private static final ConcurrentHashMap<String, AuthResponse> authCache = new ConcurrentHashMap<>();
    
    private static final AtomicInteger loginAttemptsCount = new AtomicInteger(0);

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtTokenProvider tokenProvider,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        AuthResponse response = new AuthResponse(jwt, user.getEmail(), user.getName(), user.getRole().name());
        
        authCache.put(user.getEmail(), response);
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(35);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return response;
    }

    public AuthResponse login(LoginRequest request) {
        loginAttemptsCount.incrementAndGet();
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        AuthResponse response = new AuthResponse(jwt, user.getEmail(), user.getName(), user.getRole().name());
        
        authCache.put(user.getEmail(), response);
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return response;
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate OTP (mock implementation)
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        
        // In a real application, store OTP in database with expiry
        // For now, just send email
        
        CompletableFuture.runAsync(() -> {
            emailService.sendPasswordResetEmail(user.getEmail(), otp);
        });
    }
} 