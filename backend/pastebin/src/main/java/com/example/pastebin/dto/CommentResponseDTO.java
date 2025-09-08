package com.example.pastebin.dto;

import java.time.Instant;

public record CommentResponseDTO(
        String username,
        String text,
        Instant createdAt
) {}
