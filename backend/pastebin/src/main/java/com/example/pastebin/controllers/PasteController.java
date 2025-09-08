package com.example.pastebin.controllers;

import com.example.pastebin.config.PasteProps;
import com.example.pastebin.dto.CommentResponseDTO;
import com.example.pastebin.dto.CreateCommentRequestDTO;
import com.example.pastebin.dto.CreatePasteRequestDTO;
import com.example.pastebin.dto.PasteResponseDTO;
import com.example.pastebin.service.PasteService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/paste")
public class PasteController {

    private final PasteService pasteService;
    private final PasteProps props;

    public PasteController(PasteService pasteService, PasteProps props) {
        this.pasteService = pasteService;
        this.props = props;
    }

    @PostMapping
    public ResponseEntity<PasteResponseDTO> create(@Valid @RequestBody CreatePasteRequestDTO req) {
        PasteResponseDTO created = pasteService.createPaste(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.uniqueUrl())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{uniqueUrl}")
    public PasteResponseDTO get(@PathVariable("uniqueUrl") String uniqueUrl) {
        return pasteService.getPaste(uniqueUrl);
    }

    @PostMapping("/{uniqueUrl}/comments")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable("uniqueUrl") String uniqueUrl, @Valid @RequestBody CreateCommentRequestDTO req) {
        CommentResponseDTO comment = pasteService.addComment(uniqueUrl, req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(comment);
    }

    @GetMapping("/{uniqueUrl}/comments")
    public List<CommentResponseDTO> listComments(
            @PathVariable("uniqueUrl") String uniqueUrl,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return pasteService.listComments(uniqueUrl, page, size);
    }
}
