package com.heliozz10.debetter.repository.user;

import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
public interface UserTournamentRoleRepository extends JpaRepository<UserTournamentRole, UserTournamentKey> {
    @Query("""
        select case when count(utr) > 0 then true else false end
        from UserTournamentRole utr
        where utr.user.id = :userId
              and utr.tournament.id = :tournamentId
              and utr.role in :roles
        """)
    boolean existsByUserAndTournamentAndRoleIn(
            @Param("userId") Long userId,
            @Param("tournamentId") Long tournamentId,
            @Param("roles") Collection<TournamentRole> roles
    );

    @Query("select utr.role from UserTournamentRole utr where utr.user.id = :userId and utr.tournament.id = :tournamentId")
    Set<TournamentRole> findRolesByUserIdAndTournamentId(@Param("userId") Long userId, @Param("tournamentId") Long tournamentId);

    @Query("""
        select case when count(utr) > 0 then true else false end
        from UserTournamentRole utr
        join utr.tournament t
        join RoundGroup rg on rg.tournament = t
        join Round r on r.roundGroup = rg
        where r.id = :roundId
          and utr.user.id = :userId
          and utr.role in (:roles)
        """)
    boolean existsPermissionForRound(@Param("userId") Long userId,
                                 @Param("roundId") Long roundId,
                                 @Param("roles") Collection<TournamentRole> roles);

    default boolean existsViewPermissionForRound(Long userId, Long roundId) {
        return existsPermissionForRound(userId, roundId,
                java.util.List.of(TournamentRole.VIEW, TournamentRole.EDIT, TournamentRole.FULL));
    }

    void deleteById_UserIdAndId_TournamentIdAndRole(Long userId, Long tournamentId, TournamentRole role);
}