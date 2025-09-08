package com.example.pastebin.exeption;

public class PasteNotFoundException extends RuntimeException {
    public PasteNotFoundException(String uniqueUrl) {
        super("Paste not found: " + uniqueUrl);
    }
}
