package com.heliozz10.debetter.repository.util.request;

import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizerInvitationRepository extends JpaRepository<OrganizerInvitation, Long> {
    Page<OrganizerInvitation> findByInviterId(Long inviterId, Pageable pageable);
    Page<OrganizerInvitation> findByInviteeId(Long inviteeId, Pageable pageable);
    Page<OrganizerInvitation> findByTournamentId(Long tournamentId, Pageable pageable);

    @Query("""
            select count(o) from OrganizerInvitation o
            where o.inviter.id = ?1 and o.invitee.id = ?2 and o.tournament.id = ?3""")
    long countExistingInvitation(Long id, Long id1, Long id2);
}
