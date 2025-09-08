package com.example.pastebin.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "paste_content")
public class PasteContent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String uniqueUrl;

    private String content;

    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    private List<Comment> comments = new ArrayList<>();

    public static class Comment {
        private String username;
        private String text;
        private Instant createdAt;

        public Comment() {}
        public Comment(String username, String text, Instant createdAt) {
            this.username = username; this.text = text; this.createdAt = createdAt;
        }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public PasteContent() {}
    public PasteContent(String uniqueUrl, String content, Instant expiresAt) {
        this.uniqueUrl = uniqueUrl; this.content = content; this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getUniqueUrl() { return uniqueUrl; }
    public void setUniqueUrl(String uniqueUrl) { this.uniqueUrl = uniqueUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}
