package com.example.pastebin.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    private String username;
    private String content;
}
