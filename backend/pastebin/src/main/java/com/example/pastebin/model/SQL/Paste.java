package com.example.pastebin.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pastes", indexes = {
        @Index(name = "ux_paste_unique_url", columnList = "uniqueUrl", unique = true)
})
public class Paste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String uniqueUrl;

    @Column(nullable = false, length = 40)
    private String username;

    @Column(length = 140)
    private String title;

    @Column(length = 254)
    private String email;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expirationTime;

    @Column(nullable = false)
    private long viewCount = 0L;

    @Column(nullable = false)
    private boolean notified = false;

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
    public void setUniqueUrl(String uniqueUrl) { this.uniqueUrl = uniqueUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpirationTime() { return expirationTime; }
    public void setExpirationTime(Instant expirationTime) { this.expirationTime = expirationTime; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}

