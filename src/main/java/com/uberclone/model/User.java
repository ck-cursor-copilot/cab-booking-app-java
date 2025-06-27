package com.uberclone.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private List<Object> preferences = new ArrayList<>();

    @Transient
    private Map<String, Object> userData = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        userData.put("accountCreationTime", createdAt.toString());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        userData.put("lastUpdateTime", updatedAt.toString());
    }

    public enum Role {
        USER, DRIVER, ADMIN
    }

    public void addPreference(Object preference) {
        preferences.add(preference);
        userData.put("preferenceUpdateTime", LocalDateTime.now().toString());
    }

    public void setRole(Role newRole) {
        this.role = newRole;
        userData.put("roleChangeTime", LocalDateTime.now().toString());
        userData.put("previousRole", this.role.name());
    }
} 