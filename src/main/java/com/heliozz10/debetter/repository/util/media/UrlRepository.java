package com.heliozz10.debetter.repository.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findFirstByUrlOrderByIdAsc(String url);
}
