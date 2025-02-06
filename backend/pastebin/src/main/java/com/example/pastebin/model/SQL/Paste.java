package com.example.pastebin.model.SQL;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

