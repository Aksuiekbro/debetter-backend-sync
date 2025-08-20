package com.heliozz10.debetter.repository.specification.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.tournament.in.TournamentParticipantGetParams;
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

public class TournamentParticipantSpecification {
    public static Specification<TournamentParticipant> filterBy(Long tournamentId, TournamentParticipantGetParams params, EntityManager entityManager) {
        return (Root<TournamentParticipant> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.minSpeakerScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("speakerScore"), params.minSpeakerScore()));
            }
            if (params.maxSpeakerScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("speakerScore"), params.maxSpeakerScore()));
            }
            if (tournamentId != null) {
                predicates.add(cb.equal(root.get("team").get("tournament").get("id"), tournamentId));
            }

            boolean searchFlag =
                    StringUtils.hasText(params.searchUsername()) ||
                            StringUtils.hasText(params.searchFirstName()) ||
                            StringUtils.hasText(params.searchLastName()) ||
                            StringUtils.hasText(params.searchEmail());

            if (searchFlag) {
                SearchSession searchSession = Search.session(entityManager);

                SearchResult<Long> searchResult = searchSession.search(TournamentParticipant.class)
                        .select(f -> f.id(Long.class))
                        .where(f -> {
                            var boolQuery = f.bool();

                            if (StringUtils.hasText(params.searchUsername())) {
                                boolQuery.should(f.match().field("participantProfile.user.username").matching(params.searchUsername()));
                            }
                            if (StringUtils.hasText(params.searchFirstName())) {
                                boolQuery.should(f.match().field("participantProfile.user.firstName").matching(params.searchFirstName()));
                            }
                            if (StringUtils.hasText(params.searchLastName())) {
                                boolQuery.should(f.match().field("participantProfile.user.lastName").matching(params.searchLastName()));
                            }
                            if (StringUtils.hasText(params.searchEmail())) {
                                boolQuery.should(f.match().field("participantProfile.user.email").matching(params.searchEmail()));
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
