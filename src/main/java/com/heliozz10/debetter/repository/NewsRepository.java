package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.util.media.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long>, JpaSpecificationExecutor<News> {
    @EntityGraph(value = "News.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Optional<News> findById(Long aLong);

    @EntityGraph(value = "News.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<News> findAll(Specification<News> spec, Pageable pageable);

    Optional<News> findByAuthorIdAndId(Long authorId, Long id);

    List<News> findByAuthorId(Long authorId);

    @Query("SELECT n.thumbnailUrl FROM News n WHERE n.id = :newsId")
    Optional<Url> findThumbnailUrlByNewsId(@Param("newsId") Long newsId);

    @Query("SELECT n.images FROM News n WHERE n.id = :newsId")
    List<Url> findImagesByNewsId(@Param("newsId") Long newsId);
}
