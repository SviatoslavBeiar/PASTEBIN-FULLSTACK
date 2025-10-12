package com.example.pastebin.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PasteService {

    private static final Logger log = LoggerFactory.getLogger(PasteService.class);
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasteRepository pasteRepository;
    private final PasteContentRepository pasteContentRepository;
    private final EmailService emailService;
    private final PasteProps props;

    public PasteService(PasteRepository pasteRepository,
                        PasteContentRepository pasteContentRepository,
                        EmailService emailService,
                        PasteProps props) {
        this.pasteRepository = pasteRepository;
        this.pasteContentRepository = pasteContentRepository;
        this.emailService = emailService;
        this.props = props;
    }

    @Transactional
    public PasteResponseDTO createPaste(CreatePasteRequestDTO req) {
        Instant now = Instant.now();
        long minutes = Optional.ofNullable(req.expirationMinutes()).orElse(props.getExpirationDefaultMinutes());
        Instant expiration = now.plus(minutes, ChronoUnit.MINUTES);

        String uniqueUrl = generateUniqueUrl(8);

        Paste entity = new Paste(uniqueUrl, req.username(), req.title(), req.email(), now, expiration);
        entity = pasteRepository.save(entity);

        try {
            PasteContent content = new PasteContent(uniqueUrl, req.content(), expiration.truncatedTo(ChronoUnit.SECONDS));
            pasteContentRepository.save(content);
        } catch (Exception ex) {
            // compensation: rollback SQL if Mongo failed (no XA)
            pasteRepository.deleteById(entity.getId());
            throw ex;
        }

        return new PasteResponseDTO(
                entity.getUniqueUrl(),
                entity.getTitle(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getExpirationTime(),
                entity.getViewCount(),
                req.content(),
                List.of()
        );
    }

    //SOFT DELETE behavior

    @Transactional
    public PasteResponseDTO getPaste(String uniqueUrl) {
        Instant now = Instant.now();

        Paste paste = pasteRepository.findActiveByUniqueUrl(uniqueUrl, now)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        PasteContent content = pasteContentRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        pasteRepository.incrementViews(uniqueUrl);

        List<CommentResponseDTO> comments = new ArrayList<>();
        if (content.getComments() != null) {
            for (PasteContent.Comment c : content.getComments()) {
                comments.add(new CommentResponseDTO(c.getUsername(), c.getText(), c.getCreatedAt()));
            }
        }

        return new PasteResponseDTO(
                paste.getUniqueUrl(),
                paste.getTitle(),
                paste.getUsername(),
                paste.getEmail(),
                paste.getCreatedAt(),
                paste.getExpirationTime(),
                paste.getViewCount() + 1,
                content.getContent(),
                comments
        );
    }


    @Transactional
    public CommentResponseDTO addComment(String uniqueUrl, CreateCommentRequestDTO req) {
        Instant now = Instant.now();


        pasteRepository.findActiveByUniqueUrl(uniqueUrl, now)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        PasteContent content = pasteContentRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        PasteContent.Comment comment = new PasteContent.Comment(req.username(), req.text(), Instant.now());
        if (content.getComments() == null) content.setComments(new ArrayList<>());
        content.getComments().add(comment);
        pasteContentRepository.save(content);

        return new CommentResponseDTO(comment.getUsername(), comment.getText(), comment.getCreatedAt());
    }


    @Transactional(readOnly = true)
    public List<CommentResponseDTO> listComments(String uniqueUrl, int page, int size) {
        Instant now = Instant.now();


        pasteRepository.findActiveByUniqueUrl(uniqueUrl, now)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        PasteContent content = pasteContentRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException(uniqueUrl));

        List<PasteContent.Comment> all = content.getComments() == null ? List.of() : content.getComments();
        int from = Math.max(0, page * size);
        int to = Math.min(all.size(), from + size);
        List<CommentResponseDTO> out = new ArrayList<>();
        for (int i = from; i < to; i++) {
            PasteContent.Comment c = all.get(i);
            out.add(new CommentResponseDTO(c.getUsername(), c.getText(), c.getCreatedAt()));
        }
        return out;
    }

    private String generateUniqueUrl(int length) {
        String candidate;
        int attempts = 0;
        do {
            candidate = randomBase62(length);
            attempts++;
            Assert.state(attempts < 1000, "Failed to generate unique URL after many attempts");
        } while (pasteRepository.existsByUniqueUrl(candidate));
        return candidate;
    }

    private static String randomBase62(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = BASE62[RANDOM.nextInt(BASE62.length)];
        }
        return new String(buf);
    }

    @Scheduled(fixedRateString = "#{@pasteProps.getSchedulerFixedRateMs()}")
    @Transactional
    public void processPastes() {
        Instant now = Instant.now();
        long windowMinutes = props.getNotifyWindowMinutes();
        Instant from = now;
        Instant to = now.plus(windowMinutes, ChronoUnit.MINUTES);

        var toNotify = pasteRepository.findPastesToNotify(from, to);
        for (Paste p : toNotify) {
            if (!p.isNotified()) {
                emailService.sendExpirationSoonEmail(p);
                p.setNotified(true);
                pasteRepository.save(p);
            }
        }
    }
}

