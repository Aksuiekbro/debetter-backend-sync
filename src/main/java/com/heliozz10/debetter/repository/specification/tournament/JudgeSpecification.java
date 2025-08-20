package com.heliozz10.debetter.repository.specification.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.tournament.in.JudgeGetParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JudgeSpecification {
    public static Specification<Judge> filterBy(Long tournamentId, JudgeGetParams params, EntityManager entityManager) {
        return (Root<Judge> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(params.phoneNumber())) {
                predicates.add(cb.equal(root.get("phoneNumber"), params.phoneNumber()));
            }

            if (params.checkedIn() != null) {
                predicates.add(cb.equal(root.get("checkedIn"), params.checkedIn()));
            }

            if (tournamentId != null) {
                predicates.add(cb.equal(root.get("tournament").get("id"), tournamentId));
            }

            boolean searchFlag =
                    StringUtils.hasText(params.searchFullName()) ||
                            StringUtils.hasText(params.searchEmail()) ||
                            StringUtils.hasText(params.searchSocialProfileHandle());

            if (searchFlag) {
                SearchSession searchSession = Search.session(entityManager);

                SearchResult<Long> searchResult = searchSession.search(Judge.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> {
                            var boolQuery = f.bool();

                            if (StringUtils.hasText(params.searchFullName())) {
                                boolQuery.should(f.match().field("fullName").matching(params.searchFullName()));
                            }
                            if (StringUtils.hasText(params.searchEmail())) {
                                boolQuery.should(f.match().field("email").matching(params.searchEmail()));
                            }
                            if (StringUtils.hasText(params.searchSocialProfileHandle())) {
                                boolQuery.should(f.match().field("socialProfiles.handle").matching(params.searchSocialProfileHandle()));
                            }
                            return boolQuery;
                        })
                        .fetchAll();

                List<Long> matchedIds = searchResult.hits();
                if (!matchedIds.isEmpty()) {
                    predicates.add(root.get("id").in(matchedIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
