package com.example.pastebin.repo;

import com.example.pastebin.model.noSQL.PasteContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasteContentRepository extends MongoRepository<PasteContent, String> {

}
