package com.example.pastebin.repository;


import com.example.pastebin.dto.PasteProjection;
import com.example.pastebin.model.SQL.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, Long> {
    Optional<Paste> findByUniqueUrl(String uniqueUrl);
    boolean existsByUniqueUrl(String uniqueUrl);

    @Query("select p from Paste p where p.uniqueUrl = :uniqueUrl")
    Optional<PasteProjection> findProjectedByUniqueUrl(@Param("uniqueUrl") String uniqueUrl);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Paste p set p.viewCount = p.viewCount + 1 where p.uniqueUrl = :uniqueUrl")
    int incrementViews(@Param("uniqueUrl") String uniqueUrl);

    @Query("select p from Paste p where p.notified = false and p.email is not null and p.expirationTime between :from and :to")
    List<Paste> findPastesToNotify(@Param("from") Instant from, @Param("to") Instant to);


    @Query("select p from Paste p where p.uniqueUrl = :uniqueUrl and p.expirationTime > :now")
    Optional<Paste> findActiveByUniqueUrl(@Param("uniqueUrl") String uniqueUrl,
                                          @Param("now") Instant now);
}
