package com.example.pastebin;

import com.example.pastebin.config.PasteProps;
import com.example.pastebin.dto.CommentResponseDTO;
import com.example.pastebin.dto.CreateCommentRequestDTO;
import com.example.pastebin.dto.CreatePasteRequestDTO;
import com.example.pastebin.dto.PasteResponseDTO;
import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.Paste;
import com.example.pastebin.model.PasteContent;
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
    @Mock
    EmailService emailService;
    @Mock PasteProps props;

    @Captor ArgumentCaptor<Paste> pasteCaptor;
    @Captor ArgumentCaptor<PasteContent> contentCaptor;

    @InjectMocks
    PasteService service;

    // ---------- helpers ----------

    private Paste makePaste(String url, boolean notified) {
        Paste p = new Paste(url, "user", "title", "u@example.com",
                Instant.now(), Instant.now().plus(60, ChronoUnit.MINUTES));
        p.setNotified(notified);
        return p;
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
    @DisplayName("createPaste: успіх, з дефолтним TTL з конфігів")
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
    @DisplayName("createPaste: колізія унікального коду → повторна генерація і успіх")
    void createPaste_collisionThenSuccess() {
        when(props.getExpirationDefaultMinutes()).thenReturn(60L);
        // перший раз — URL зайнятий, далі — вільний
        when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(true, false);
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePasteRequestDTO req = new CreatePasteRequestDTO("body", null, "u", null, 5L);
        PasteResponseDTO dto = service.createPaste(req);

        assertNotNull(dto.uniqueUrl());
        verify(pasteRepository, atLeast(2)).existsByUniqueUrl(anyString());
        verify(pasteRepository).save(any(Paste.class));
        // довжина коду 8 згідно generateUniqueUrl(8)
        assertEquals(8, dto.uniqueUrl().length());
    }

    @Test
    @DisplayName("createPaste: помилка Mongo → компенсаційне deleteById у MySQL")
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
    @DisplayName("getPaste: успіх, інкремент переглядів після успішного фетчу")
    void getPaste_ok_increments() {
        String url = "abcd1234";
        Paste p = makePaste(url, false);
        PasteContent c = makeContent(url, 2);

        when(pasteRepository.findByUniqueUrl(url)).thenReturn(Optional.of(p));
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
    @DisplayName("getPaste: немає Paste у MySQL → PasteNotFoundException")
    void getPaste_missingSql_throws() {
        when(pasteRepository.findByUniqueUrl("nope")).thenReturn(Optional.empty());
        assertThrows(PasteNotFoundException.class, () -> service.getPaste("nope"));
        verifyNoInteractions(pasteContentRepository);
    }

    @Test
    @DisplayName("getPaste: немає контенту в Mongo → PasteNotFoundException")
    void getPaste_missingMongo_throws() {
        String url = "u";
        when(pasteRepository.findByUniqueUrl(url)).thenReturn(Optional.of(makePaste(url, false)));
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.empty());
        assertThrows(PasteNotFoundException.class, () -> service.getPaste(url));
        verify(pasteRepository, never()).incrementViews(anyString());
    }

    @Test
    @DisplayName("addComment: додає коментар і зберігає контент; ініціалізує список якщо null")
    void addComment_ok_initializesList() {
        String url = "xyz";
        Paste p = makePaste(url, false);
        PasteContent c = makeContent(url, 0);
        c.setComments(null); // емулюємо null у БД

        when(pasteRepository.findByUniqueUrl(url)).thenReturn(Optional.of(p));
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
    @DisplayName("listComments: пагінація працює (page=1,size=2 → 1 елемент)")
    void listComments_pagination() {
        String url = "pg";
        PasteContent c = makeContent(url, 3); // індекси 0,1,2
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));

        List<CommentResponseDTO> page = service.listComments(url, 1, 2);
        assertEquals(1, page.size());
        assertEquals("u2", page.get(0).username());
        assertEquals("t2", page.get(0).text());
    }

    @Test
    @DisplayName("listComments: порожній список коли немає коментарів")
    void listComments_empty() {
        String url = "empty";
        PasteContent c = makeContent(url, 0);
        c.setComments(null); // явний null
        when(pasteContentRepository.findByUniqueUrl(url)).thenReturn(Optional.of(c));

        List<CommentResponseDTO> page = service.listComments(url, 0, 10);
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("processPastes: надсилає листи і відмічає notified=true для кандидатів")
    void processPastes_sendsAndMarks() {
        when(props.getNotifyWindowMinutes()).thenReturn(30L);

        Paste p1 = makePaste("a1", false);
        Paste p2 = makePaste("a2", true);  // вже notified → пропустити
        when(pasteRepository.findPastesToNotify(any(), any())).thenReturn(List.of(p1, p2));
        when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processPastes();

        verify(emailService, times(1)).sendExpirationSoonEmail(p1);
        verify(emailService, never()).sendExpirationSoonEmail(p2);

        verify(pasteRepository).save(pasteCaptor.capture());
        assertTrue(pasteCaptor.getValue().isNotified());
    }

    @Nested
    @DisplayName("createPaste: поведінка TTL/часів")
    class TimeBehavior {
        @Test
        @DisplayName("коли клієнт передає expirationMinutes=null → береться з конфігів")
        void usesDefaultTtlWhenNull() {
            when(props.getExpirationDefaultMinutes()).thenReturn(90L);
            when(pasteRepository.existsByUniqueUrl(anyString())).thenReturn(false);
            when(pasteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(pasteContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreatePasteRequestDTO req = new CreatePasteRequestDTO("x", null, "u", null, null);
            PasteResponseDTO dto = service.createPaste(req);

            assertNotNull(dto.expirationTime());
            assertNotNull(dto.createdAt());
            // просто sanity-check, що expiry пізніше createdAt
            assertTrue(dto.expirationTime().isAfter(dto.createdAt()));
        }
    }
}
