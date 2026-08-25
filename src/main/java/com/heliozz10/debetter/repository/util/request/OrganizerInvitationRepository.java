package com.heliozz10.debetter.repository.util.request;

import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizerInvitationRepository extends JpaRepository<OrganizerInvitation, Long> {
    @EntityGraph(value = "OrganizerInvitation.withInviteeAndTournament", type = EntityGraph.EntityGraphType.LOAD)
    Optional<OrganizerInvitation> findByInviteeIdAndId(Long inviteeId, Long id);

    Optional<OrganizerInvitation> findRawByInviteeIdAndId(Long inviteeId, Long id);

    @EntityGraph(value = "OrganizerInvitation.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            select o from OrganizerInvitation o
            where o.inviter.id = :inviterId
              and (o.tournament.disabled = false or o.tournament.disabled is null)
            """)
    Page<OrganizerInvitation> findByInviterId(@Param("inviterId") Long inviterId, Pageable pageable);

    @EntityGraph(value = "OrganizerInvitation.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            select o from OrganizerInvitation o
            where o.invitee.id = :inviteeId
              and (o.tournament.disabled = false or o.tournament.disabled is null)
            """)
    Page<OrganizerInvitation> findByInviteeId(@Param("inviteeId") Long inviteeId, Pageable pageable);

    @EntityGraph(value = "OrganizerInvitation.forView", type = EntityGraph.EntityGraphType.LOAD)
    Page<OrganizerInvitation> findByTournamentId(Long tournamentId, Pageable pageable);

    @Query("""
            select count(o) from OrganizerInvitation o
            where o.inviter.id = ?1 and o.invitee.user.username = ?2 and o.tournament.id = ?3""")
    long countExistingInvitations(Long id, String username, Long id1);

    @Query("""
            select o from OrganizerInvitation o
            where o.inviter.id = ?1 and o.invitee.user.username = ?2 and o.tournament.id = ?3""")
    Optional<OrganizerInvitation> findExistingInvitation(Long inviterId, String inviteeUsername, Long tournamentId);
}
