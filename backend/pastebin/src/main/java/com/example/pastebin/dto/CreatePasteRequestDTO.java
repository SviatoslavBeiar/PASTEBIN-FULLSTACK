package com.example.pastebin.dto;

import jakarta.validation.constraints.*;

public record CreatePasteRequestDTO(
        @NotBlank @Size(max = 12000) String content,
        @Size(max = 140) String title,
        @NotBlank @Size(max = 40) String username,
        @Email String email,
        @PositiveOrZero Long expirationMinutes
) {}
