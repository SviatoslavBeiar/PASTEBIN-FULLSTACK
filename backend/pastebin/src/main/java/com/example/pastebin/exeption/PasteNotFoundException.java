package com.example.pastebin.exeption;

public class PasteNotFoundException extends RuntimeException {
    
    public PasteNotFoundException(String message) {
        super(message);
    }
}
