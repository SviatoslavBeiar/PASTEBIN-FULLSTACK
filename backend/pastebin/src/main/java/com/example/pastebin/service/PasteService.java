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

        PasteContent.Comment comment = new PasteContent.Comment();
        comment.setUsername(username);
        comment.setContent(content);
        comment.setTimestamp(LocalDateTime.now());

        List<PasteContent.Comment> comments = Optional.ofNullable(pasteContent.getComments())
                .orElseGet(ArrayList::new);
        comments.add(comment);
        pasteContent.setComments(comments);

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
        Paste paste = new Paste();
        paste.setTitle(title);
        paste.setUsername(username);
        paste.setEmail(email);
        paste.setUniqueUrl(UUID.randomUUID().toString());

        long minutesToExpire = Optional.ofNullable(expirationTime).orElse(DEFAULT_EXPIRATION_MINUTES);
        paste.setExpirationTime(LocalDateTime.now().plusMinutes(minutesToExpire));

        Paste savedPaste = pasteRepository.save(paste);

        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(savedPaste.getUniqueUrl());
        pasteContent.setContent(content);
        pasteContent.setComments(new ArrayList<>());
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

        List<Paste> pastesToNotify = pasteRepository.findAllByExpirationTimeBeforeAndNotifiedFalse(now.plusMinutes(30));
        List<Paste> pastesToDelete = pasteRepository.findAllByExpirationTimeBefore(now);

        notifyUsersAboutExpiration(pastesToNotify, now);
        deleteExpiredPastes(pastesToDelete);
    }

    private void notifyUsersAboutExpiration(List<Paste> pastesToNotify, LocalDateTime now) {
        for (Paste paste : pastesToNotify) {
            Duration totalTime = Duration.between(paste.getCreationTime(), paste.getExpirationTime());
            Duration remainingTime = Duration.between(now, paste.getExpirationTime());

            if (remainingTime.toMinutes() <= totalTime.toMinutes() * 0.5) {
                String subject = "Your paste is about to expire";
                String text = String.format("Dear %s,\n\nYour paste with title '%s' is about to expire soon.", paste.getUsername(), paste.getTitle());

                try {
                    emailService.sendSimpleMessage(paste.getEmail(), subject, text);
                    paste.setNotified(true);
                } catch (Exception e) {
                    log.error("Failed to send email to {}", paste.getEmail(), e);
                }
            }
        }
        pasteRepository.saveAll(pastesToNotify);
    }

    private void deleteExpiredPastes(List<Paste> pastesToDelete) {
        for (Paste paste : pastesToDelete) {
            pasteRepository.delete(paste);
            pasteContentRepository.deleteById(paste.getUniqueUrl());
        }
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




