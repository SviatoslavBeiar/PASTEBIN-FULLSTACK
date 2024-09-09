package com.example.pastebin.model;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreatePasteRequest {
    private String content;
    private String title;
    private String username;
    private Long expirationTime;
    private String email;
}