package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateParticipantDto;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.service.CommonService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class TeamService {
    private final EntityManager entityManager;

    private final TeamRepository teamRepository;

    private final CommonService commonService;

    @Transactional(readOnly = true)
    public Page<Team> getTeamsByTournamentId(Long tournamentId, Pageable pageable) {
        return teamRepository.findByTournamentId(tournamentId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Club> getClubs(String searchName, Pageable pageable) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<Club> searchResult = searchSession.search(Club.class)
                .where(f -> {
                    if (StringUtils.hasText(searchName)) {
                        return f.match()
                                .field("name")
                                .matching(searchName);
                    }
                    return f.matchAll();
                })
                .sort(f -> f.field("name"))
                .fetch((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(searchResult.hits(), pageable, searchResult.total().hitCount());
    }

    @Transactional(readOnly = true)
    public Team getTeamByTournamentIdAndId(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new EntityNotFoundException("Team not found"));
    }

    @Transactional
    public int updateTeam_Organizer(TeamUpdateOrganizerDto teamUpdateOrganizerDto, Long tournamentId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if(!Objects.equals(tournamentId, team.getTournament().getId())) {
            throw new IllegalArgumentException("Team does not belong to this tournament");
        }

        return teamRepository.updateNameById(teamUpdateOrganizerDto.name(), teamId);
    }

    @Transactional
    public int updateTeam_Participant(TeamUpdateParticipantDto teamUpdateParticipantDto, Long tournamentId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if(!Objects.equals(tournamentId, team.getTournament().getId())) {
            throw new IllegalArgumentException("Team does not belong to this tournament");
        }

        return teamRepository.updateNameAndClubById(teamUpdateParticipantDto.name(), commonService.findOrCreateEntity(teamUpdateParticipantDto.club(), Club.class, entityManager), teamId);
    }
    /**
     * Validates that adding one more member won't exceed max size.
     */
    public void validateTeamSize(Team team) {
        int potentialTeamSize = team.getMembers().size() + 1;
        int maxSize = getMaxTeamSize(team.getTournament().getPreliminaryFormat());

        if (potentialTeamSize > maxSize) {
            throw new IllegalArgumentException("Team is already full");
        }
    }

    public int getMaxTeamSize(DebateFormat format) {
        return (format == DebateFormat.KP) ? 3 : 2;
    }
}
