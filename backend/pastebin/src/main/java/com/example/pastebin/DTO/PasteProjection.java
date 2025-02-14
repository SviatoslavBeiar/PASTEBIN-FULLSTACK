package com.example.pastebin.DTO;

import java.time.LocalDateTime;

public interface PasteProjection {
    String getUniqueUrl();
    String getTitle();
    String getUsername();
    LocalDateTime getExpirationTime();
    long getViewCount();
}
