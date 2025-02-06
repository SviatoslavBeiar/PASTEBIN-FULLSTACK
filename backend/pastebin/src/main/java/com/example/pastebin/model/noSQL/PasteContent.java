package com.example.pastebin.model.noSQL;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "paste_content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasteContent {
    @Id
    private String id;
    private String content;
    private List<Comment> comments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comment {
        private String username;
        private String content;
        private LocalDateTime timestamp;
    }
}
