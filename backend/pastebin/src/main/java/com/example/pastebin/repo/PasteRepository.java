package com.example.pastebin.repo;

import com.example.pastebin.model.SQL.Paste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, Long> {
     Optional<Paste> findByUniqueUrl(String uniqueUrl);
    List<Paste> findAllByExpirationTimeBeforeAndNotifiedFalse(LocalDateTime expirationTime);
    List<Paste> findAllByExpirationTimeBefore(LocalDateTime expirationTime);
}
