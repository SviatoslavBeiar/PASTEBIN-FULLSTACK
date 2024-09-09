package com.example.pastebin.service;

import com.example.pastebin.exeption.PasteNotFoundException;
import com.example.pastebin.model.noSQL.PasteContent;
import com.example.pastebin.model.SQL.Paste;
import com.example.pastebin.repo.PasteContentRepository;
import com.example.pastebin.repo.PasteRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PasteService {

    private final PasteRepository pasteRepository;
    private final PasteContentRepository pasteContentRepository;
    private final EmailService emailService;

    public PasteContent.Comment addComment(String uniqueUrl, String username, String content) {
        PasteContent pasteContent = pasteContentRepository.findById(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste content not found for URL: " + uniqueUrl));

        PasteContent.Comment comment = new PasteContent.Comment();
        comment.setUsername(username);
        comment.setContent(content);
        comment.setTimestamp(LocalDateTime.now());

        List<PasteContent.Comment> comments = pasteContent.getComments();
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
        pasteContent.setComments(comments);

        pasteContentRepository.save(pasteContent);

        return comment;
    }

    public List<PasteContent.Comment> getCommentsByPaste(String uniqueUrl) {
        PasteContent pasteContent = pasteContentRepository.findById(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste content not found for URL: " + uniqueUrl));
        return pasteContent.getComments();
    }

    public Paste incrementViewCount(String uniqueUrl) {
        Paste paste = pasteRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste not found for URL: " + uniqueUrl));
        paste.setViewCount(paste.getViewCount() + 1);
        return pasteRepository.save(paste);
    }

    public Paste createPaste(String content, String title, String username, String email, Long expirationTime) {
        Paste paste = new Paste();
        paste.setTitle(title);
        paste.setUsername(username);
        paste.setEmail(email);
        paste.setUniqueUrl(UUID.randomUUID().toString());

        long minutesToExpire = expirationTime != null ? expirationTime : 2;
        paste.setExpirationTime(LocalDateTime.now().plusMinutes(minutesToExpire));

        Paste savedPaste = pasteRepository.save(paste);

        PasteContent pasteContent = new PasteContent();
        pasteContent.setId(savedPaste.getUniqueUrl());
        pasteContent.setContent(content);
        pasteContent.setComments(new ArrayList<>());  // Initialize comments as an empty list
        pasteContentRepository.save(pasteContent);

        return savedPaste;
    }

    public Paste getPasteMetadata(String uniqueUrl) {
        return pasteRepository.findByUniqueUrl(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste not found for URL: " + uniqueUrl));
    }

    public PasteContent getPasteContent(String uniqueUrl) {
        return pasteContentRepository.findById(uniqueUrl)
                .orElseThrow(() -> new PasteNotFoundException("Paste content not found for URL: " + uniqueUrl));
    }

    @Scheduled(fixedRate = 30000)
    public void processPastes() {
        LocalDateTime now = LocalDateTime.now();

        // Extract pastes that may need notification or removal
        List<Paste> pastesToNotify = pasteRepository.findAllByExpirationTimeBeforeAndNotifiedFalse(now.plusMinutes(30));
        List<Paste> pastesToDelete = pasteRepository.findAllByExpirationTimeBefore(now);

        for (Paste paste : pastesToNotify) {
            Duration totalTime = Duration.between(paste.getCreationTime(), paste.getExpirationTime());
            Duration remainingTime = Duration.between(now, paste.getExpirationTime());

            if (remainingTime.toMinutes() <= totalTime.toMinutes() * 0.5) {
                String subject = "Your paste is about to expire";
                String text = "Dear " + paste.getUsername() + ",\n\nYour paste with title '" + paste.getTitle() + "' is about to expire soon.";
                emailService.sendSimpleMessage(paste.getEmail(), subject, text);
                paste.setNotified(true);
            }
        }

        pasteRepository.saveAll(pastesToNotify);

        for (Paste paste : pastesToDelete) {
            pasteRepository.delete(paste);
            pasteContentRepository.deleteById(paste.getUniqueUrl());
        }
    }



}


