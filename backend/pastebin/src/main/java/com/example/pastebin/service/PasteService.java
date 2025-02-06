package com.example.pastebin.service;

import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.repo.PasteContentRepository;
import com.example.pastebin.repo.PasteRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class PasteService {

    private final PasteRepository pasteRepository;
    private final PasteContentRepository pasteContentRepository;
    private final EmailService emailService;

    private static final long DEFAULT_EXPIRATION_MINUTES = 2;

    public PasteContent.Comment addComment(String uniqueUrl, String username, String content) {

        PasteContent pasteContent = findPasteContentById(uniqueUrl);

        PasteContent.Comment comment = PasteContent.Comment.builder()
                .username(username)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        List<PasteContent.Comment> comments = pasteContent.getComments();
        if (comments == null) {
            comments = new ArrayList<>();
            pasteContent.setComments(comments);
        }

        comments.add(comment);
        pasteContentRepository.save(pasteContent);

        return comment;
    }


    public List<PasteContent.Comment> getCommentsByPaste(String uniqueUrl) {
        PasteContent pasteContent = findPasteContentById(uniqueUrl);
        return pasteContent.getComments();
    }

    public Paste incrementViewCount(String uniqueUrl) {
        Paste paste = findPasteByUniqueUrl(uniqueUrl);
        paste.setViewCount(paste.getViewCount() + 1);
        return pasteRepository.save(paste);
    }

    public Paste createPaste(String content, String title, String username, String email, Long expirationTime) {

        long minutesToExpire = Optional.ofNullable(expirationTime).orElse(DEFAULT_EXPIRATION_MINUTES);

        Paste paste = Paste.builder()
                .title(title)
                .username(username)
                .email(email)
                .uniqueUrl(UUID.randomUUID().toString())
                .expirationTime(LocalDateTime.now().plusMinutes(minutesToExpire))
                .viewCount(0)
                .notified(false)
                .build();

        Paste savedPaste = pasteRepository.save(paste);

        PasteContent pasteContent = PasteContent.builder()
                .id(savedPaste.getUniqueUrl())
                .content(content)
                .comments(new ArrayList<>())
                .build();

        pasteContentRepository.save(pasteContent);

        return savedPaste;
    }


    public Paste getPasteMetadata(String uniqueUrl) {
        return findPasteByUniqueUrl(uniqueUrl);
    }
    public PasteContent getPasteContent(String uniqueUrl) {
        return findPasteContentById(uniqueUrl);
    }

    @Scheduled(fixedRate = 30000)
    public void processPastes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notifyThreshold = now.plusMinutes(30);

        List<Paste> pastesToNotify = pasteRepository.findAllByExpirationTimeBeforeAndNotifiedFalse(notifyThreshold);
        List<Paste> pastesToDelete = pasteRepository.findAllByExpirationTimeBefore(now);

        pastesToNotify.forEach(paste -> {
            Duration totalTime = Duration.between(paste.getCreationTime(), paste.getExpirationTime());
            Duration remainingTime = Duration.between(now, paste.getExpirationTime());

            if (remainingTime.toMinutes() <= totalTime.toMinutes() * 0.5) {
                String subject = "Your paste is about to expire";
                String text = String.format("Dear %s,\n\nYour paste with title '%s' is about to expire soon.",
                        paste.getUsername(), paste.getTitle());
                try {
                    emailService.sendSimpleMessage(paste.getEmail(), subject, text);
                    paste.setNotified(true);
                } catch (Exception e) {
                    log.error("Failed to send email to {}", paste.getEmail(), e);
                }
            }
        });
        pasteRepository.saveAll(pastesToNotify);

        pastesToDelete.forEach(paste -> {
            pasteRepository.delete(paste);
            pasteContentRepository.deleteById(paste.getUniqueUrl());
        });
    }


    private Paste findPasteByUniqueUrl(String uniqueUrl) {
        return pasteRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste not found for URL: " + uniqueUrl));
    }
    private PasteContent findPasteContentById(String uniqueUrl) {
        return pasteContentRepository.findById(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste content not found for URL: " + uniqueUrl));
    }
}



