package com.example.pastebin.repository;

import com.example.pastebin.model.PasteContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PasteContentRepository extends MongoRepository<PasteContent, String> {
    Optional<PasteContent> findByUniqueUrl(String uniqueUrl);
}
