package com.example.pastebin.model;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PasteResponse {
    private String content;
    private String title;
    private String username;
    private LocalDateTime expirationTime;
    private long viewCount;
}