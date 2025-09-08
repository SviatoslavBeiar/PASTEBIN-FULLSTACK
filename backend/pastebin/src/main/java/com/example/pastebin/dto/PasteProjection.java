package com.example.pastebin.web;

import java.time.Instant;

public interface PasteProjection {
    String getUniqueUrl();
    String getTitle();
    String getUsername();
    Instant getCreatedAt();
    long getViewCount();
}
