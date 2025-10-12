package com.example.pastebin.model.noSQL;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*; // <— додано
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Document(collection = "paste_content")
public class PasteContent {

    @Id
    private String id;

    @Setter
    @NotBlank
    @Indexed(unique = true)
    private String uniqueUrl;

    @Setter
    @NotBlank
    private String content;

    @Setter
    @Indexed(name = "idx_expires_at", expireAfterSeconds = 0)
    private Instant expiresAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    @Setter
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


    public void addComment(Comment comment) {
        this.comments.add(comment);
    }
}
