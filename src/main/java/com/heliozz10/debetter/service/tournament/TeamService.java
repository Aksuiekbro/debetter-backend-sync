package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.dto.tournament.team.in.ParticipantSelectorDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateParticipantDto;
import com.heliozz10.debetter.dto.tournament.team.out.TeamView;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class TeamService {
    private final EntityManager entityManager;

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final ParticipantProfileRepository participantProfileRepository;
    private final UserRepository userRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;

    private final CommonService commonService;

    private final TournamentParticipantService tournamentParticipantService;
    private final TournamentSecurity tournamentSecurity;

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

        if(StringUtils.hasText(teamUpdateOrganizerDto.name())) {
            team.setName(teamUpdateOrganizerDto.name().trim());
        }

        if(teamUpdateOrganizerDto.club() != null) {
            String clubName = teamUpdateOrganizerDto.club().trim();
            team.setClub(clubName.isEmpty()
                    ? null
                    : commonService.findOrCreateEntity(clubName, Club.class, entityManager));
        }

        if(teamUpdateOrganizerDto.members() != null) {
            replaceMembers(tournamentId, team, teamUpdateOrganizerDto.members());
        }

        teamRepository.save(team);
        return 1;
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

    private void replaceMembers(Long tournamentId, Team team, List<ParticipantSelectorDto> selectors) {
        if(selectors.isEmpty()) {
            throw new IllegalArgumentException("Team must have at least one participant");
        }

        int maxTeamSize = getMaxTeamSize(team.getTournament().getPreliminaryFormat());
        if(selectors.size() > maxTeamSize) {
            throw new IllegalArgumentException("Team can have at most " + maxTeamSize + " participants");
        }

        List<ParticipantProfile> desiredProfiles = resolveUniqueParticipantProfiles(selectors);
        Map<Long, TournamentParticipant> currentByProfileId = new LinkedHashMap<>();
        for(TournamentParticipant member : new ArrayList<>(team.getMembers())) {
            currentByProfileId.put(member.getParticipantProfile().getId(), member);
        }

        for(ParticipantProfile profile : desiredProfiles) {
            ensureParticipantAvailableForTeam(tournamentId, team, profile);
        }

        for(TournamentParticipant existingMember : new ArrayList<>(team.getMembers())) {
            Long profileId = existingMember.getParticipantProfile().getId();
            boolean shouldRemain = desiredProfiles.stream()
                    .anyMatch(profile -> Objects.equals(profile.getId(), profileId));

            if(!shouldRemain) {
                team.getMembers().remove(existingMember);
                tournamentSecurity.removeRoleFromUser(existingMember.getParticipantProfile().getUser().getId(), tournamentId, TournamentRole.VIEW);
                tournamentParticipantRepository.delete(existingMember);
            }
        }

        for(ParticipantProfile profile : desiredProfiles) {
            if(currentByProfileId.containsKey(profile.getId())) {
                continue;
            }

            TournamentParticipant participant = new TournamentParticipant();
            participant.setTeam(team);
            participant.setParticipantProfile(profile);
            participant.setSpeakerScore(0);
            team.getMembers().add(participant);
            tournamentParticipantRepository.save(participant);
            tournamentSecurity.assignRoleToUser(profile.getUser().getId(), tournamentId, TournamentRole.VIEW);
        }

        team.setActive(true);
        team.setCheckedIn(false);
    }

    private List<ParticipantProfile> resolveUniqueParticipantProfiles(List<ParticipantSelectorDto> selectors) {
        Map<Long, ParticipantProfile> profilesById = new LinkedHashMap<>();

        for(ParticipantSelectorDto selector : selectors) {
            ParticipantProfile profile = resolveParticipantProfile(selector);

            if(profilesById.putIfAbsent(profile.getId(), profile) != null) {
                throw new IllegalArgumentException("A participant cannot be listed twice in the same team");
            }
        }

        return new ArrayList<>(profilesById.values());
    }

    private ParticipantProfile resolveParticipantProfile(ParticipantSelectorDto selector) {
        Profile profile;

        if(selector.id() != null) {
            profile = participantProfileRepository.findById(selector.id())
                    .orElseThrow(() -> new EntityNotFoundException("Participant not found"));
        } else if(StringUtils.hasText(selector.username())) {
            User user = userRepository.findByUsername(selector.username())
                    .orElseThrow(() -> new EntityNotFoundException("Participant not found"));
            profile = user.getProfile();
        } else {
            throw new IllegalArgumentException("Invalid participant selector");
        }

        if(!(profile instanceof ParticipantProfile participantProfile)) {
            throw new IllegalArgumentException("Trying to add a non-participant profile");
        }

        return participantProfile;
    }

    private void ensureParticipantAvailableForTeam(Long tournamentId, Team team, ParticipantProfile profile) {
        tournamentParticipantRepository
                .findByTeam_Tournament_IdAndParticipantProfile_Id(tournamentId, profile.getId())
                .ifPresent(existingParticipant -> {
                    if(!Objects.equals(existingParticipant.getTeam().getId(), team.getId())) {
                        throw new IllegalArgumentException("Participant is already registered for another team in this tournament");
                    }
                });
    }
}
