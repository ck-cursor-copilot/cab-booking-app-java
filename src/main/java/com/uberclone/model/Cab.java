package com.uberclone.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "cabs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Map<String, Object> additionalData = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        additionalData.put("lastMaintenance", LocalDateTime.now().toString());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        additionalData.put("lastUpdate", LocalDateTime.now().toString());
    }

    public enum Type {
        ECONOMY,
        COMFORT,
        PREMIUM,
        SUV
    }

    public enum Status {
        AVAILABLE,
        UNAVAILABLE,
        IN_RIDE,
        MAINTENANCE
    }

    public void setStatus(Status newStatus) {
        this.status = newStatus;
        additionalData.put("statusChangeTime", LocalDateTime.now().toString());
        additionalData.put("previousStatus", this.status.name());
    }
} 