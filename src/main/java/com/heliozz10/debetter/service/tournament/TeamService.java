package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateParticipantDto;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
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

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class TeamService {
    private final EntityManager entityManager;

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;

    private final CommonService commonService;

    private final TournamentParticipantService tournamentParticipantService;

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
    public Team getTeamByTournamentIdAndId(Long tournamentId, Long teamId) {
        return teamRepository.findByTournamentIdAndId(tournamentId, teamId).orElseThrow(() -> new EntityNotFoundException("Team not found"));
    }

    @Transactional
    public int updateTeam_Organizer(TeamUpdateOrganizerDto teamUpdateOrganizerDto, Long tournamentId, Long teamId) {
        Team team = teamRepository.findByTournamentIdAndId(tournamentId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        return teamRepository.updateNameById(teamUpdateOrganizerDto.name() != null ? teamUpdateOrganizerDto.name() : team.getName(), teamId);
    }

    @Transactional
    public int updateTeam_Participant(TeamUpdateParticipantDto teamUpdateParticipantDto, Long tournamentId, Long teamId, Long memberId) {
        Team team = teamRepository.findByTournament_IdAndMembers_IdAndId(tournamentId, memberId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        return teamRepository.updateNameAndClubById(
                teamUpdateParticipantDto.name() != null ? teamUpdateParticipantDto.name() : team.getName(),
                teamUpdateParticipantDto.club() != null ? commonService.findOrCreateEntity(teamUpdateParticipantDto.club(), Club.class, entityManager) : team.getClub(),
                teamId
        );
    }

    public TeamView toTeamView(Team team) {
        TeamView view = teamMapper.toTeamView(team);
        view.setMembers(team.getMembers().stream().map(tournamentParticipantService::toSimpleTournamentParticipantView).toList());
        return view;
    }

    public List<TeamView> toTeamViews(List<Team> teams) { return teams.stream().map(this::toTeamView).toList(); }

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
