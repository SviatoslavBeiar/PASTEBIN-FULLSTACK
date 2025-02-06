package com.example.pastebin.DTO;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequestDTO {
    private String username;
    private String content;
}
