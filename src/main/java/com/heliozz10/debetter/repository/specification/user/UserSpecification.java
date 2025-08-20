package com.heliozz10.debetter.repository.specification.user;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.user.in.UserGetParams;
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

public class UserSpecification {
    public static Specification<User> filterBy(UserGetParams params, EntityManager entityManager) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<Long> matchedIds = new ArrayList<>();
            SearchSession searchSession = Search.session(entityManager);

            boolean searchFlag =
                    StringUtils.hasText(params.searchUsername()) ||
                            StringUtils.hasText(params.searchFirstName()) ||
                            StringUtils.hasText(params.searchLastName()) ||
                            StringUtils.hasText(params.searchEmail()) ||
                            StringUtils.hasText(params.searchSocialProfileHandle());

            if (searchFlag) {
                SearchResult<Long> searchResult = searchSession.search(User.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> {
                            var boolQuery = f.bool();

                            if (StringUtils.hasText(params.searchUsername())) {
                                boolQuery.should(f.match()
                                        .field("username")
                                        .matching(params.searchUsername()));
                            }
                            if (StringUtils.hasText(params.searchFirstName())) {
                                boolQuery.should(f.match()
                                        .field("firstName")
                                        .matching(params.searchFirstName()));
                            }
                            if (StringUtils.hasText(params.searchLastName())) {
                                boolQuery.should(f.match()
                                        .field("lastName")
                                        .matching(params.searchLastName()));
                            }
                            if (StringUtils.hasText(params.searchEmail())) {
                                boolQuery.should(f.match()
                                        .field("email")
                                        .matching(params.searchEmail()));
                            }
                            if (StringUtils.hasText(params.searchSocialProfileHandle())) {
                                boolQuery.should(f.match()
                                        .field("socialProfiles.handle")
                                        .matching(params.searchSocialProfileHandle()));
                            }

                            return boolQuery;
                        })
                        .fetchAll();

                matchedIds = searchResult.hits();
                if (!matchedIds.isEmpty()) {
                    predicates.add(root.get("id").in(matchedIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            if (params.role() != null) {
                predicates.add(cb.equal(root.get("role"), params.role()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
