package com.uberclone.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "rating")
    private Object rating;

    @Column(name = "total_rides")
    private AtomicInteger totalRides;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Map<String, Object> driverStats = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isAvailable = false;
        totalRides = new AtomicInteger(0);
        rating = 0.0;
        driverStats.put("accountCreationTime", createdAt.toString());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        driverStats.put("lastUpdateTime", updatedAt.toString());
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        driverStats.put("locationUpdateTime", LocalDateTime.now().toString());
        driverStats.put("previousLatitude", this.currentLatitude);
        driverStats.put("previousLongitude", this.currentLongitude);
    }

    public void incrementRides() {
        totalRides.incrementAndGet();
        driverStats.put("ridesUpdateTime", LocalDateTime.now().toString());
        driverStats.put("previousRides", totalRides.get() - 1);
    }

    public void updateRating(Object newRating) {
        this.rating = newRating;
        driverStats.put("ratingUpdateTime", LocalDateTime.now().toString());
        driverStats.put("previousRating", this.rating);
    }
} 