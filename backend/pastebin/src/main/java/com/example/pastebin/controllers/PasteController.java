package com.example.pastebin.controllers;

import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.CreateCommentRequest;
import com.example.pastebin.model.CreatePasteRequest;
import com.example.pastebin.model.PasteResponse;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.service.PasteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paste")
public class PasteController {

    private final PasteService pasteService;

    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }

    @PostMapping("/{uniqueUrl}/comments")
    public ResponseEntity<PasteContent.Comment> addComment(
            @PathVariable String uniqueUrl,
            @RequestBody CreateCommentRequest commentRequest) {

        PasteContent.Comment comment = pasteService.addComment(
                uniqueUrl,
                commentRequest.getUsername(),
                commentRequest.getContent()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/{uniqueUrl}/comments")
    public ResponseEntity<List<PasteContent.Comment>> getComments(@PathVariable String uniqueUrl) {
        List<PasteContent.Comment> comments = pasteService.getCommentsByPaste(uniqueUrl);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<String> createPaste(@RequestBody CreatePasteRequest pasteRequest) {
        Paste paste = pasteService.createPaste(
                pasteRequest.getContent(),
                pasteRequest.getTitle(),
                pasteRequest.getUsername(),
                pasteRequest.getEmail(),  // Добавляем email
                pasteRequest.getExpirationTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(paste.getUniqueUrl());
    }

    @GetMapping("/{uniqueUrl}")
    public ResponseEntity<PasteResponse> getPaste(@PathVariable String uniqueUrl) {
        System.out.println("Getting paste: " + uniqueUrl);
        pasteService.incrementViewCount(uniqueUrl);

        Paste paste = pasteService.getPasteMetadata(uniqueUrl);
        PasteContent pasteContent = pasteService.getPasteContent(uniqueUrl);

        if (paste != null && pasteContent != null) {
            PasteResponse response = new PasteResponse(
                    pasteContent.getContent(),
                    paste.getTitle(),
                    paste.getUsername(),
                    paste.getExpirationTime(),
                    paste.getViewCount()
            );
            return ResponseEntity.ok(response);
        } else {
            throw new PasteNotFoundException("Paste not found for URL: " + uniqueUrl);
        }
    }
}
