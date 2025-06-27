package com.uberclone.driver.controller;

import com.uberclone.driver.dto.DriverRegistrationRequest;
import com.uberclone.driver.dto.DriverResponse;
import com.uberclone.driver.dto.DriverStatusRequest;
import com.uberclone.driver.dto.LocationUpdateRequest;
import com.uberclone.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver", description = "Driver management APIs")
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Register as a driver")
    public ResponseEntity<DriverResponse> registerDriver(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DriverRegistrationRequest request) {
        return ResponseEntity.ok(driverService.registerDriver(userDetails.getUsername(), request));
    }

    @PutMapping("/status")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update driver status")
    public ResponseEntity<DriverResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DriverStatusRequest request) {
        return ResponseEntity.ok(driverService.updateStatus(userDetails.getUsername(), request));
    }

    @PutMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update driver location")
    public ResponseEntity<DriverResponse> updateLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(driverService.updateLocation(userDetails.getUsername(), request));
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get driver earnings")
    public ResponseEntity<Double> getEarnings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(driverService.getEarnings(userDetails.getUsername()));
    }

    @GetMapping("/rides")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get driver ride history")
    public ResponseEntity<List<BookingResponse>> getRideHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(driverService.getRideHistory(userDetails.getUsername()));
    }
} 