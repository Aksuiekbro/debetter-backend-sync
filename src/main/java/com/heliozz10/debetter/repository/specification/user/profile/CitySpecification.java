package com.heliozz10.debetter.repository.specification.user.profile;

import com.heliozz10.debetter.content.user.profile.City;
import com.heliozz10.debetter.dto.user.profile.in.CityGetParams;
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

public class CitySpecification {
    public static Specification<City> filterBy(CityGetParams params, EntityManager entityManager) {
        return (Root<City> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(params.searchName())) {
                SearchSession searchSession = Search.session(entityManager);
                SearchResult<Long> searchResult = searchSession.search(City.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> f.match()
                                .field("name")
                                .matching(params.searchName()))
                        .fetchAll();

                List<Long> matchedIds = searchResult.hits();
                if (!matchedIds.isEmpty()) {
                    predicates.add(root.get("id").in(matchedIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));        };
    }
}
