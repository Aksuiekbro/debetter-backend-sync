package com.heliozz10.debetter.repository.specification;

import com.heliozz10.debetter.content.News;
import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.dto.in.NewsGetParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NewsSpecification {
    public static Specification<News> filterBy(NewsGetParams params, EntityManager entityManager) {
        return (Root<News> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(params.searchTitle())) {
                SearchSession searchSession = Search.session(entityManager);

                SearchResult<Long> searchResult = searchSession.search(News.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> f.match()
                                .field("title")
                                .matching(params.searchTitle()))
                        .fetchAll();

                List<Long> matchedIds = searchResult.hits();
                if (!matchedIds.isEmpty()) {
                    predicates.add(root.get("id").in(matchedIds));
                } else {
                    return cb.disjunction();
                }
            }

            if (params.tags() != null && !params.tags().isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<News> subRoot = subquery.from(News.class);
                Join<News, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"))
                        .where(cb.and(
                                cb.equal(subRoot.get("id"), root.get("id")),
                                subTags.get("name").in(params.tags())
                        ))
                        .groupBy(subRoot.get("id"))
                        .having(cb.equal(cb.countDistinct(subTags.get("name")), params.tags().size()));

                predicates.add(cb.exists(subquery));
            }

            if (params.authorId() != null) {
                predicates.add(cb.equal(root.get("author").get("id"), params.authorId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
