package com.heliozz10.debetter.repository.specification.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Principal-bound tournament membership rules for the authenticated "mine" view.
 */
public final class TournamentMembershipSpecification {
    private static final List<TournamentRole> ACCEPTED_ROLES = List.of(
            TournamentRole.VIEW,
            TournamentRole.EDIT,
            TournamentRole.FULL
    );

    private static final List<TournamentRole> HIDDEN_TOURNAMENT_ROLES = List.of(
            TournamentRole.EDIT,
            TournamentRole.FULL
    );

    private TournamentMembershipSpecification() {
    }

    public static Specification<Tournament> visibleMembershipsFor(Long userId) {
        return (root, query, cb) -> {
            query.distinct(true);

            Join<Tournament, UserTournamentRole> membership = root.join("tournamentRoles", JoinType.INNER);
            Predicate belongsToUser = cb.equal(membership.get("user").get("id"), userId);
            Predicate isAcceptedMembership = membership.get("role").in(ACCEPTED_ROLES);
            Predicate isPubliclyVisible = cb.or(
                    cb.isFalse(root.get("disabled")),
                    cb.isNull(root.get("disabled"))
            );
            Predicate canManageHiddenTournament = membership.get("role").in(HIDDEN_TOURNAMENT_ROLES);

            return cb.and(
                    belongsToUser,
                    isAcceptedMembership,
                    cb.or(isPubliclyVisible, canManageHiddenTournament)
            );
        };
    }
}
