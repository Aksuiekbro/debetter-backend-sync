package com.heliozz10.debetter.repository.specification.tournament;

import com.heliozz10.debetter.content.tag.Tag;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TournamentSpecification {
    public static Specification<Tournament> filterBy(TournamentGetParams params, EntityManager entityManager) {
        return (Root<Tournament> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<Long> matchedIds = new ArrayList<>();
            SearchSession searchSession = Search.session(entityManager);

            boolean searchFlag = StringUtils.hasText(params.searchName()) || StringUtils.hasText(params.searchLocation());

            if(searchFlag) {
                SearchResult<Long> searchResult = searchSession.search(Tournament.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> {
                            var boolQuery = f.bool();

                            if(StringUtils.hasText(params.searchName())) {
                                boolQuery.should(f.match().field("name").matching(params.searchName()));
                            }

                            if(StringUtils.hasText(params.searchLocation())) {
                                boolQuery.should(f.wildcard().field("location").matching(params.searchLocation()));
                            }

                            return boolQuery;
                        })
                        .fetchAll();
                matchedIds = searchResult.hits();
                if(!matchedIds.isEmpty()) {
                    predicates.add(cb.in(root.get("id")).value(matchedIds));
                } else {
                    return cb.disjunction();
                }
            }

            if (params.tags() != null && !params.tags().isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Tournament> subRoot = subquery.from(Tournament.class);
                Join<Tournament, Tag> subTags = subRoot.join("tags");

                subquery.select(subRoot.get("id"))
                        .where(cb.and(
                                subRoot.get("id").in(root.get("id")),
                                subTags.get("name").in(params.tags())
                        ))
                        .groupBy(subRoot.get("id"))
                        .having(cb.equal(cb.countDistinct(subTags.get("name")), params.tags().size()));

                predicates.add(cb.exists(subquery));
            }

            if(params.startDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), params.startDateFrom()));
            }

            if(params.startDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), params.startDateTo()));
            }

            if(params.registrationDeadlineFrom() != null) {
                predicates.add((cb.greaterThanOrEqualTo(root.get("registrationDeadline"), params.registrationDeadlineFrom())));
            }

            if(params.registrationDeadlineTo() != null) {
                predicates.add((cb.lessThanOrEqualTo(root.get("registrationDeadline"), params.registrationDeadlineTo())));
            }

            if(params.league() != null) {
                predicates.add(cb.equal(root.get("league"), params.league()));
            }

            if(params.nonFull() != null && params.nonFull()) {
                predicates.add(cb.lessThan(cb.size(root.get("teams")), root.get("teamLimit")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
