package com.example.pastebin.model.SQL;

import jakarta.persistence.*;
import jakarta.validation.constraints.*; // <— додано
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pastes", indexes = {
        @Index(name = "ux_paste_unique_url", columnList = "uniqueUrl", unique = true),
        @Index(name = "ix_paste_username", columnList = "username"),
        @Index(name = "ix_paste_created_at", columnList = "createdAt")
})
public class Paste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64)
    private String uniqueUrl;

    @Setter
    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String username;

    @Setter
    @Size(max = 140)
    @Column(length = 140)
    private String title;

    @Setter
    @Email
    @Size(max = 254)
    @Column(length = 254)
    private String email;

    @Setter
    @Column(nullable = false)
    private Instant createdAt;

    @Setter
    @FutureOrPresent
    @Column(nullable = false)
    private Instant expirationTime;

    @Setter
    @Column(nullable = false)
    private long viewCount = 0L;

    @Setter
    @Column(nullable = false)
    private boolean notified = false;

    @Setter
    @Version
    private long version;

    public Paste() {}

    public Paste(String uniqueUrl, String username, String title, String email, Instant createdAt, Instant expirationTime) {
        this.uniqueUrl = uniqueUrl;
        this.username = username;
        this.title = title;
        this.email = email;
        this.createdAt = createdAt;
        this.expirationTime = expirationTime;
    }

    public Long getId() { return id; }
    public String getUniqueUrl() { return uniqueUrl; }
    public String getUsername() { return username; }
    public String getTitle() { return title; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpirationTime() { return expirationTime; }
    public long getViewCount() { return viewCount; }
    public boolean isNotified() { return notified; }
    public long getVersion() { return version; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (expirationTime != null && !expirationTime.isAfter(createdAt)) {
            throw new IllegalStateException("expirationTime must be after createdAt");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (expirationTime != null && createdAt != null && !expirationTime.isAfter(createdAt)) {
            throw new IllegalStateException("expirationTime must be after createdAt");
        }
    }


    public void incrementViewCount() { this.viewCount++; }
    public void markNotified() { this.notified = true; }
}
