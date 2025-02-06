package com.example.pastebin.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PasteResponseDTO {
    private String content;
    private String title;
    private String username;
    private LocalDateTime expirationTime;
    private long viewCount;
}