package com.heliozz10.debetter.repository.util.request;

import com.heliozz10.debetter.content.util.request.ParticipantInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantInvitationRepository extends JpaRepository<ParticipantInvitation, Long> {
    Page<ParticipantInvitation> findByInviterId(Long inviterId, Pageable pageable);
    Page<ParticipantInvitation> findByInviteeId(Long inviteeId, Pageable pageable);
    Page<ParticipantInvitation> findByTeamId(Long teamId, Pageable pageable);

    long countByTeam_Id(Long id);

    @Query("""
            select count(p) from ParticipantInvitation p
            where p.inviter.id = ?1 and p.team.id = ?2 and p.invitee.id = ?3""")
    long countExistingInvitation(Long id, Long id1, Long id2);
}
