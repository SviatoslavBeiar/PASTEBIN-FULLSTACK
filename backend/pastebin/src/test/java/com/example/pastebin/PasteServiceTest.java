package com.example.pastebin;

import com.example.pastebin.config.PasteProps;
import com.example.pastebin.dto.CommentResponseDTO;
import com.example.pastebin.dto.CreateCommentRequestDTO;
import com.example.pastebin.dto.CreatePasteRequestDTO;
import com.example.pastebin.dto.PasteResponseDTO;
import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.repository.PasteContentRepository;
import com.example.pastebin.repository.PasteRepository;
import com.example.pastebin.service.EmailService;
import com.example.pastebin.service.PasteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasteServiceTest {

    @Mock PasteRepository pasteRepository;
    @Mock PasteContentRepository pasteContentRepository;
    @Mock EmailService emailService;
    @Mock PasteProps props;

    @Captor ArgumentCaptor<Paste> pasteCaptor;
    @Captor ArgumentCaptor<PasteContent> contentCaptor;

    @InjectMocks PasteService service;

    // ---------- helpers ----------

    private Paste makePaste(String url, boolean notified) {
        Paste p = new Paste(url, "user", "title", "u@example.com",
                Instant.now(), Instant.now().plus(60, ChronoUnit.MINUTES));
        p.setNotified(notified);
        return p;
    }

    private Paste makeExpiredPaste(String url) {

        return new Paste(url, "user", "title", "u@example.com",
                Instant.now().minus(120, ChronoUnit.MINUTES),
                Instant.now().minus(60, ChronoUnit.MINUTES));
    }

    private PasteContent makeContent(String url, int comments) {
        PasteContent c = new PasteContent(url, "content-" + url,
                Instant.now().plus(60, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        if (comments > 0) {
            List<PasteContent.Comment> list = new ArrayList<>();
            for (int i = 0; i < comments; i++) {
                list.add(new PasteContent.Comment("u" + i, "t" + i, Instant.now().minusSeconds(i)));
            }
            c.setComments(list);
        }
        return c;
    }

    // ---------- tests ----------

    @Test
    @DisplayName("createPaste: success with default TTL from config")
    void createPaste_ok() {
        when(props.getExpirationDefaultMinutes()).thenReturn(60L);
        when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(false);
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteRequestDTO req = new CreatePasteRequestDTO("hello", "t", "u", "mail@ex.com", null);

        PasteResponseDTO dto = service.createPaste(req);

        assertNotNull(dto.uniqueUrl());
        assertEquals("u", dto.username());
        assertEquals("hello", dto.content());
        verify(pasteRepository).save(pasteCaptor.capture());
        Paste saved = pasteCaptor.getValue();
        assertEquals(dto.uniqueUrl(), saved.getUniqueUrl());
    }

    @Test
    @DisplayName("createPaste: unique code collision -> regenerate and succeed")
    void createPaste_collisionThenSuccess() {
        when(props.getExpirationDefaultMinutes()).thenReturn(60L);

        when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(true, false);
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteRequestDTO req = new CreatePasteRequestDTO("body", null, "u", null, 5L);
        PasteResponseDTO dto = service.createPaste(req);

        assertNotNull(dto.uniqueUrl());
        verify(pasteRepository, atLeast(2)).existsByUniqueUrl(anyString());
        verify(pasteRepository).save(any(Paste.class));
        // length is 8 as per generateUniqueUrl(8)
        assertEquals(8, dto.uniqueUrl().length());
    }

    @Test
    @DisplayName("createPaste: Mongo failure -> compensating deleteById in MySQL")
    void createPaste_mongoFails_compensates() {
        when(props.getExpirationDefaultMinutes()).thenReturn(60L);
        when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(false);
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pasteContentRepository.save(any())).thenThrow(new RuntimeException("mongo down"));

        CreatePasteRequestDTO req = new CreatePasteRequestDTO("x", null, "u", null, null);
        assertThrows(RuntimeException.class, () -> service.createPaste(req));

        verify(pasteRepository).deleteById(any());
    }

    @Test
    @DisplayName("getPaste: success (active), increments views after successful fetch")
    void getPaste_ok_increments() {
        String url = "abcd1234";
        Paste p = makePaste(url, false);
        PasteContent c = makeContent(url, 2);

        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.of(p));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));
        when(pasteRepository.incrementViews(url)).thenReturn(1);

        PasteResponseDTO dto = service.getPaste(url);

        assertEquals(url, dto.uniqueUrl());
        assertEquals(c.getContent(), dto.content());
        assertEquals(p.getViewCount() + 1, dto.viewCount());
        assertEquals(2, dto.comments().size());
        verify(pasteRepository).incrementViews(url);
    }

    @Test
    @DisplayName("getPaste: expired -> PasteNotFoundException")
    void getPaste_expired_throws() {
        String url = "expired";

        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.empty());
        assertThrows(PasteNotFoundException.class, () -> service.getPaste(url));
        verifyNoInteractions(pasteContentRepository);
        verify(pasteRepository, never()).incrementViews(anyString());
    }

    @Test
    @DisplayName("getPaste: missing content in Mongo -> PasteNotFoundException")
    void getPaste_missingMongo_throws() {
        String url = "u";
        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.of(makePaste(url, false)));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.empty());
        assertThrows(PasteNotFoundException.class, () -> service.getPaste(url));
        verify(pasteRepository, never()).incrementViews(anyString());
    }

    @Test
    @DisplayName("addComment: adds comment and saves content; initializes list if null (active)")
    void addComment_ok_initializesList() {
        String url = "xyz";
        Paste p = makePaste(url, false);
        PasteContent c = makeContent(url, 0);
        c.setComments(null);

        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.of(p));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));
        when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCommentRequestDTO req = new CreateCommentRequestDTO("bob", "hi");
        CommentResponseDTO out = service.addComment(url, req);

        assertEquals("bob", out.username());
        assertEquals("hi", out.text());

        verify(pasteContentRepository).save(contentCaptor.capture());
        PasteContent saved = contentCaptor.getValue();
        assertNotNull(saved.getComments());
        assertEquals(1, saved.getComments().size());
        assertEquals("bob", saved.getComments().get(0).getUsername());
    }

    @Test
    @DisplayName("addComment: expired -> PasteNotFoundException (soft delete)")
    void addComment_expired_throws() {
        String url = "exp";
        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.empty());
        CreateCommentRequestDTO req = new CreateCommentRequestDTO("bob", "hi");
        assertThrows(PasteNotFoundException.class, () -> service.addComment(url, req));
        verifyNoInteractions(pasteContentRepository);
    }

    @Test
    @DisplayName("listComments: pagination works (active) (page=1,size=2 -> 1 item)")
    void listComments_pagination() {
        String url = "pg";
        PasteContent c = makeContent(url, 3); // indexes 0,1,2
        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.of(makePaste(url, false)));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));

        List<CommentResponseDTO> page = service.listComments(url, 1, 2);
        assertEquals(1, page.size());
        assertEquals("u2", page.get(0).username());
        assertEquals("t2", page.get(0).text());
    }

    @Test
    @DisplayName("listComments: empty list when there are no comments (active)")
    void listComments_empty() {
        String url = "empty";
        PasteContent c = makeContent(url, 0);
        c.setComments(null);
        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.of(makePaste(url, false)));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));

        List<CommentResponseDTO> page = service.listComments(url, 0, 10);
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("listComments: expired -> PasteNotFoundException (soft delete)")
    void listComments_expired_throws() {
        String url = "gone";
        when(pasteRepository.findActiveByUniqueUrl(eq(url), any(Instant.class))).thenReturn(Optional.empty());
        assertThrows(PasteNotFoundException.class, () -> service.listComments(url, 0, 10));
        verifyNoInteractions(pasteContentRepository);
    }

    @Test
    @DisplayName("processPastes: sends emails and marks notified=true for candidates")
    void processPastes_sendsAndMarks() {
        when(props.getNotifyWindowMinutes()).thenReturn(30L);

        Paste p1 = makePaste("a1", false);
        Paste p2 = makePaste("a2", true);  // already notified -> skip
        when(pasteRepository.findPastesToNotify(any(), any())).thenReturn(List.of(p1, p2));
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processPastes();

        verify(emailService, times(1)).sendExpirationSoonEmail(p1);
        verify(emailService, never()).sendExpirationSoonEmail(p2);

        verify(pasteRepository).save(pasteCaptor.capture());
        assertTrue(pasteCaptor.getValue().isNotified());
    }

    @Nested
    @DisplayName("createPaste: TTL/time behavior")
    class TimeBehavior {
        @Test
        @DisplayName("when client passes expirationMinutes=null -> taken from config")
        void usesDefaultTtlWhenNull() {
            when(props.getExpirationDefaultMinutes()).thenReturn(90L);
            when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(false);
            when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreatePasteRequestDTO req = new CreatePasteRequestDTO("x", null, "u", null, null);
            PasteResponseDTO dto = service.createPaste(req);

            assertNotNull(dto.expirationTime());
            assertNotNull(dto.createdAt());
            assertTrue(dto.expirationTime().isAfter(dto.createdAt()));
        }
    }
}

