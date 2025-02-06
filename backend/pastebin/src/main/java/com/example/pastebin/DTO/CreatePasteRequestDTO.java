package com.example.pastebin.DTO;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreatePasteRequestDTO {
    private String content;
    private String title;
    private String username;
    private Long expirationTime;
    private String email;
}