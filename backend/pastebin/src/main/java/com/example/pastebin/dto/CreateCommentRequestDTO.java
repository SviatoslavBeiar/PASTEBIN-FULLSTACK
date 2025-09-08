package com.example.pastebin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequestDTO(
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Size(max = 4000) String text
) {}
