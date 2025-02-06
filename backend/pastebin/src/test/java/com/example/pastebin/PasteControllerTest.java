package com.example.pastebin;

import com.example.pastebin.controllers.PasteController;
import com.example.pastebin.DTO.CreateCommentRequestDTO;
import com.example.pastebin.DTO.CreatePasteRequestDTO;
import com.example.pastebin.DTO.PasteResponseDTO;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.service.PasteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasteControllerTest {

    @Mock
    private PasteService pasteService;

    @InjectMocks
    private PasteController pasteController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddComment() {
        String uniqueUrl = "test-url";
        CreateCommentRequestDTO commentRequest = new CreateCommentRequestDTO("user", "This is a comment");

        // Use builder to create expected comment instance.
        PasteContent.Comment comment = PasteContent.Comment.builder()
                .username("user")
                .content("This is a comment")
                .timestamp(LocalDateTime.now())
                .build();

        when(pasteService.addComment(uniqueUrl, "user", "This is a comment")).thenReturn(comment);

        ResponseEntity<PasteContent.Comment> response = pasteController.addComment(uniqueUrl, commentRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user", response.getBody().getUsername());
        assertEquals("This is a comment", response.getBody().getContent());
    }

    @Test
    void testGetComments() {
        String uniqueUrl = "test-url";
        List<PasteContent.Comment> comments = new ArrayList<>();
        comments.add(PasteContent.Comment.builder().build());

        when(pasteService.getCommentsByPaste(uniqueUrl)).thenReturn(comments);

        ResponseEntity<List<PasteContent.Comment>> response = pasteController.getComments(uniqueUrl);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testCreatePaste() {
        CreatePasteRequestDTO pasteRequest = new CreatePasteRequestDTO("Content", "Title", "user", 120L, "email@example.com");
        String uniqueUrl = "test-url";
        Paste paste = new Paste();
        paste.setUniqueUrl(uniqueUrl);

        when(pasteService.createPaste("Content", "Title", "user", "email@example.com", 120L)).thenReturn(paste);

        ResponseEntity<String> response = pasteController.createPaste(pasteRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(uniqueUrl, response.getBody());
    }

    @Test
    void testGetPaste() {
        String uniqueUrl = "test-url";
        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(uniqueUrl);
        Paste paste = new Paste();
        paste.setUniqueUrl(uniqueUrl);
        paste.setTitle("Title");
        paste.setUsername("user");
        paste.setExpirationTime(LocalDateTime.now().plusDays(1));
        paste.setViewCount(0);
        PasteResponseDTO pasteResponse = new PasteResponseDTO(
                "Content",
                "Title",
                "user",
                paste.getExpirationTime(),
                paste.getViewCount()
        );

        when(pasteService.incrementViewCount(uniqueUrl)).thenReturn(paste);
        when(pasteService.getPasteMetadata(uniqueUrl)).thenReturn(paste);
        when(pasteService.getPasteContent(uniqueUrl)).thenReturn(pasteContent);

        ResponseEntity<PasteResponseDTO> response = pasteController.getPaste(uniqueUrl);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Title", response.getBody().getTitle());
    }
}
