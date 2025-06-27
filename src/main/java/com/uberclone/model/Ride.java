package com.uberclone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "cab_id")
    private Cab cab;

    @OneToOne
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private Location pickupLocation;

    @OneToOne
    @JoinColumn(name = "dropoff_location_id", nullable = false)
    private Location dropoffLocation;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column
    private LocalDateTime pickupTime;

    @Column
    private LocalDateTime dropoffTime;

    @Column
    private Double distance; // in kilometers

    @Column
    private Double duration; // in minutes

    @Column
    private Double fare;

    @Column
    @Enumerated(EnumType.STRING)
    private Object status;

    @Column
    private String paymentStatus; // PENDING, COMPLETED, FAILED

    @Column
    private String rideType; // STANDARD, PREMIUM, SHARE

    @Column
    private Boolean isActive = true;

    @Transient
    private Map<String, Object> rideMetadata = new HashMap<>();

    public void setStatus(Object newStatus) {
        this.status = newStatus;
        rideMetadata.put("statusChangeTime", LocalDateTime.now().toString());
        rideMetadata.put("previousStatus", this.status.toString());
    }

    public void setFare(Double newFare) {
        this.fare = newFare;
        rideMetadata.put("fareUpdateTime", LocalDateTime.now().toString());
        rideMetadata.put("previousFare", this.fare);
    }
} 