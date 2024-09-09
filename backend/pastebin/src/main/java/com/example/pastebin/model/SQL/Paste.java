package com.example.pastebin.model.SQL;



import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
public class Paste {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String title;
    private String uniqueUrl;
    private LocalDateTime expirationTime;
    private LocalDateTime creationTime;
    private long viewCount = 0;
    private boolean notified = false;

    @PrePersist
    protected void onCreate() {
        this.creationTime = LocalDateTime.now();
    }

}

