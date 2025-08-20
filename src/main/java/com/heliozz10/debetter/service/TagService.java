package com.heliozz10.debetter.service;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tag.TagType;
import com.heliozz10.debetter.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagRepository tagRepository;

    @Transactional
    public Page<Tag> getTagsByType(TagType type, Pageable pageable) {
        return tagRepository.findByType(type, pageable);
    }

    @Transactional
    public List<Tag> findOrCreateTags(TagType type, List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tag> existingTags = tagRepository.findByTypeAndNameIn(type, names);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Tag> newTags = names.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Tag tag = new Tag();
                    tag.setType(type);
                    tag.setName(name.trim());
                    return tag;
                })
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
        }

        List<Tag> allTags = new ArrayList<>(existingTags);
        allTags.addAll(newTags);
        return allTags;
    }

    @Transactional
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }
}
