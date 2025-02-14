package com.example.pastebin.repo;

import com.example.pastebin.DTO.PasteProjection;
import com.example.pastebin.model.SQL.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, Long> {
//     Optional<Paste> findByUniqueUrl(String uniqueUrl);
//    List<Paste> findAllByExpirationTimeBeforeAndNotifiedFalse(LocalDateTime expirationTime);
//    List<Paste> findAllByExpirationTimeBefore(LocalDateTime expirationTime);


    Optional<PasteProjection> findProjectedByUniqueUrl(String uniqueUrl);

    @Query("SELECT p FROM Paste p WHERE p.uniqueUrl = :uniqueUrl")
    Optional<Paste> findByUniqueUrl(@Param("uniqueUrl") String uniqueUrl);

    @Query("SELECT p FROM Paste p WHERE p.expirationTime < :expirationTime AND p.notified = false")
    List<Paste> findAllByExpirationTimeBeforeAndNotifiedFalse(@Param("expirationTime") LocalDateTime expirationTime);

    @Query("SELECT p FROM Paste p WHERE p.expirationTime < :expirationTime")
    List<Paste> findAllByExpirationTimeBefore(@Param("expirationTime") LocalDateTime expirationTime);
}
