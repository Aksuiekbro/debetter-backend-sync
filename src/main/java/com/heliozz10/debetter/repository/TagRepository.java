package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tag.TagType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByTypeAndNameIn(TagType type, Collection<String> names);

    Page<Tag> findByType(TagType type, Pageable pageable);
}
