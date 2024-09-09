package com.example.pastebin;

import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.CreatePasteRequest;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.repo.PasteContentRepository;
import com.example.pastebin.repo.PasteRepository;
import com.example.pastebin.service.PasteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasteServiceTest {

    @Mock
    private PasteRepository pasteRepository;

    @Mock
    private PasteContentRepository pasteContentRepository;

    @Mock
    private JavaMailSender emailSender;

    @InjectMocks
    private PasteService pasteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddComment() {
        String uniqueUrl = "test-url";
        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(uniqueUrl);
        pasteContent.setComments(new ArrayList<>());

        PasteContent.Comment newComment = new PasteContent.Comment();
        newComment.setUsername("user");
        newComment.setContent("This is a comment");

        when(pasteContentRepository.findById(uniqueUrl)).thenReturn(Optional.of(pasteContent));

        PasteContent.Comment result = pasteService.addComment(uniqueUrl, "user", "This is a comment");

        assertNotNull(result);
        assertEquals("user", result.getUsername());
        assertEquals("This is a comment", result.getContent());
        verify(pasteContentRepository, times(1)).save(pasteContent);
    }

    @Test
    void testGetCommentsByPaste() {
        String uniqueUrl = "test-url";
        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(uniqueUrl);
        List<PasteContent.Comment> comments = new ArrayList<>();
        comments.add(new PasteContent.Comment());
        pasteContent.setComments(comments);

        when(pasteContentRepository.findById(uniqueUrl)).thenReturn(Optional.of(pasteContent));

        List<PasteContent.Comment> result = pasteService.getCommentsByPaste(uniqueUrl);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testIncrementViewCount() {
        String uniqueUrl = "test-url";
        Paste paste = new Paste();
        paste.setUniqueUrl(uniqueUrl);
        paste.setViewCount(0);

        when(pasteRepository.findByUniqueUrl(uniqueUrl)).thenReturn(Optional.of(paste));
        when(pasteRepository.save(paste)).thenReturn(paste);

        Paste result = pasteService.incrementViewCount(uniqueUrl);

        assertNotNull(result);
        assertEquals(1, result.getViewCount());
        verify(pasteRepository, times(1)).save(paste);
    }

    @Test
    void testCreatePaste() {
        String uniqueUrl = UUID.randomUUID().toString();
        CreatePasteRequest pasteRequest = new CreatePasteRequest();
        pasteRequest.setContent("Content");
        pasteRequest.setTitle("Title");
        pasteRequest.setUsername("user");
        pasteRequest.setEmail("email@example.com");
        pasteRequest.setExpirationTime(120L);

        Paste paste = new Paste();
        paste.setUniqueUrl(uniqueUrl);

        when(pasteRepository.save(any(Paste.class))).thenReturn(paste);
        when(pasteContentRepository.save(any(PasteContent.class))).thenReturn(new PasteContent());

        Paste result = pasteService.createPaste("Content", "Title", "user", "email@example.com", 120L);

        assertNotNull(result);
        assertEquals(uniqueUrl, result.getUniqueUrl());
        verify(pasteRepository, times(1)).save(any(Paste.class));
        verify(pasteContentRepository, times(1)).save(any(PasteContent.class));
    }

    @Test
    void testGetPasteMetadata() {
        String uniqueUrl = "test-url";
        Paste paste = new Paste();
        paste.setUniqueUrl(uniqueUrl);

        when(pasteRepository.findByUniqueUrl(uniqueUrl)).thenReturn(Optional.of(paste));

        Paste result = pasteService.getPasteMetadata(uniqueUrl);

        assertNotNull(result);
        assertEquals(uniqueUrl, result.getUniqueUrl());
    }

    @Test
    void testGetPasteContent() {
        String uniqueUrl = "test-url";
        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(uniqueUrl);

        when(pasteContentRepository.findById(uniqueUrl)).thenReturn(Optional.of(pasteContent));

        PasteContent result = pasteService.getPasteContent(uniqueUrl);

        assertNotNull(result);
        assertEquals(uniqueUrl, result.getId());
    }
}
