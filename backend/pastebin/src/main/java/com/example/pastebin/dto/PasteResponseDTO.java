package com.example.pastebin.dto;

import java.time.Instant;
import java.util.List;

public record PasteResponseDTO(
        String uniqueUrl,
        String title,
        String username,
        String email,
        Instant createdAt,
        Instant expirationTime,
        long viewCount,
        String content,
        List<CommentResponseDTO> comments
) {}
