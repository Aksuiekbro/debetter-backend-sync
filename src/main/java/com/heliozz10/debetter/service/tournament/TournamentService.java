package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.dto.tournament.in.OrganizerSelectorDto;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import com.heliozz10.debetter.dto.tournament.team.in.ParticipantSelectorDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamFormDto;
import com.heliozz10.debetter.dto.tournament.in.TournamentFormDto;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.projection.TournamentCheckResult;
import com.heliozz10.debetter.repository.specification.tournament.TournamentSpecification;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.announcement.AnnouncementRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.service.CommonService;
import com.heliozz10.debetter.service.tournament.round.RoundService;
import com.heliozz10.debetter.service.user.UserService;
import com.heliozz10.debetter.service.util.media.FileService;
import com.heliozz10.debetter.service.util.request.OrganizerInvitationService;
import com.heliozz10.debetter.service.util.request.ParticipantInvitationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//TODO: When deleting a tournament dont forget to remove the thumbnail images from the file system
//TODO: add cascade = CascadeType.ALL and add orphanRemoval = true to where needed
@RequiredArgsConstructor
@Service
public class TournamentService {
    private final EntityManager entityManager;

    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;

    private final TournamentParticipantRepository tournamentParticipantRepository;

    private final TeamRepository teamRepository;
    private final TeamService teamService;
    private final TeamMapper teamMapper;

    private final JudgeRepository judgeRepository;
    private final JudgeMapper judgeMapper;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;

    private final OrganizerProfileRepository organizerProfileRepository;

    private final ParticipantProfileRepository participantProfileRepository;

    private final OrganizerInvitationService organizerInvitationService;

    private final ParticipantInvitationService participantInvitationService;

    private final RoundGroupRepository roundGroupRepository;

    private final RoundService roundService;
    private final RoundRepository roundRepository;

    private final MatchService matchService;

    private final FileService fileService;
    private final CommonService commonService;

    //TODO: create entity graphs !important
    //TOURNAMENT RETRIEVAL

    @Transactional(readOnly = true)
    public Page<Tournament> getTournaments(TournamentGetParams params, Pageable pageable) {
        Specification<Tournament> spec = TournamentSpecification.filterBy(params, entityManager);
        return tournamentRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
    }

    //TOURNAMENT CREATION

    @Transactional
    public Tournament createTournament(TournamentFormDto dto, Long organizerId) {
        if(
                (dto.preliminaryFormat() == DebateFormat.KP && dto.teamEliminationFormat() != DebateFormat.KP) ||
                (dto.preliminaryFormat() != DebateFormat.KP && dto.teamEliminationFormat() == DebateFormat.KP)) {
            throw new IllegalArgumentException("If the preliminary format is Karl Popper, the team elimination format must also be Karl Popper and vice versa");
        }

        if(
                dto.teamLimit() < Math.pow(2, dto.eliminationRoundCount())
        ) {
            throw new IllegalArgumentException("The team limit must be at least 2^eliminationRoundCount. " + dto.eliminationRoundCount() + " elimination rounds are not possible with a team limit of " + dto.teamLimit());
        }

        OrganizerProfile organizer = organizerProfileRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found"));
        Tournament tournament = tournamentMapper.toTournament(dto);
        tournament.setMainOrganizer(organizer);
        tournament.setStarted(false);
        tournament.setFinished(false);

        if(dto.image() != null) {
            Url url = fileService.uploadFile(dto.image(), "tournaments/thumbnails", tournament.getId().toString());
            tournament.setImageUrl(url);
        }

        generateRounds(tournament, dto.preliminaryRoundCount(), dto.eliminationRoundCount());

        return tournamentRepository.save(tournament);
    }

    private void generateRounds(Tournament tournament, int preliminaryRoundCount, int eliminationRoundCount) {
        RoundGroup preliminaryRoundGroup = new RoundGroup(tournament, RoundGroupType.PRELIMINARY, tournament.getPreliminaryFormat());
        for(int i = 0; i < preliminaryRoundCount; i++) {
            Round preliminaryRound = new Round(preliminaryRoundGroup, "Round " + (i + 1), i + 1);
            preliminaryRound.setRoundGroup(preliminaryRoundGroup);
        }

        RoundGroup soloEliminationRoundGroup = new RoundGroup(tournament, RoundGroupType.SOLO_ELIMINATION, DebateFormat.LD);
        for(int i = 0; i < eliminationRoundCount; i++) {
            Round soloEliminationRound = new Round(soloEliminationRoundGroup, i == eliminationRoundCount - 1 ? "Final" : "1/" + (eliminationRoundCount - 1 - i), i + 1);
            soloEliminationRound.setRoundGroup(soloEliminationRoundGroup);
        }

        RoundGroup teamEliminationRoundGroup = new RoundGroup(tournament, RoundGroupType.TEAM_ELIMINATION, tournament.getTeamEliminationFormat());
        for(int i = 0; i < eliminationRoundCount; i++) {
            Round teamEliminationRound = new Round(teamEliminationRoundGroup, i == eliminationRoundCount - 1 ? "Final" : "1/" + (eliminationRoundCount - 1 - i), i + 1);
            teamEliminationRound.setRoundGroup(teamEliminationRoundGroup);
        }
    }

    //TOURNAMENT UPDATING

    @Transactional
    public Tournament updateTournament(TournamentFormDto dto, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
        if(tournament.getStarted()) {
            throw new IllegalStateException("Tournament has already started");
        }

        if(dto.image() != null) {
            if(tournament.getImageUrl() != null) {
                fileService.deleteFile(tournament.getImageUrl());
            }
            Url url = fileService.uploadFile(dto.image(), "tournaments/thumbnails", tournament.getId().toString());
            tournament.setImageUrl(url);
        }

        tournamentMapper.updateTournament(dto, tournament);
        tournamentRepository.save(tournament);
        return tournament;
    }

    //ORGANIZERS

    @Transactional(readOnly = true)
    public Optional<User> getMainOrganizer(Long tournamentId) {
        return tournamentRepository.findMainOrganizerByTournamentId(tournamentId);
    }

    @Transactional(readOnly = true)
    public List<User> getOrganizers(Long tournamentId) {
        return tournamentRepository.findOrganizersByTournamentId(tournamentId);
    }

    /**
     * UNUSED
     * @param organizerSelectorDto
     * @param tournamentId
     * @param inviterId
     * @return
     */
    @Transactional
    public OrganizerInvitation inviteOrganizerToTournament(OrganizerSelectorDto organizerSelectorDto, Long tournamentId, Long inviterId) {
        Profile profile;

        if(organizerSelectorDto.id() != null) {
            profile = entityManager.getReference(OrganizerProfile.class, organizerSelectorDto.id());
        } else if(organizerSelectorDto.username() != null) {
            profile = userService.loadUserByUsername(organizerSelectorDto.username()).getProfile();
        } else {
            throw new IllegalArgumentException("Invalid organizer selector");
        }

        if(!(profile instanceof OrganizerProfile organizer)) {
            throw new IllegalArgumentException("Trying to invite a non-organizer profile");
        }

        return organizerInvitationService.createInvitation(inviterId, profile.getId(), tournamentId);
    }

    @Transactional
    public void addOrganizerToTournament(Long organizerId, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        OrganizerProfile organizer = organizerProfileRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found"));

        tournament.getOrganizers().add(organizer);
        organizer.getCoOrganizedTournaments().add(tournament);
    }

    @Transactional
    public void removeOrganizerFromTournament(Long tournamentId, Long organizerId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        tournament.getOrganizers().removeIf(o -> Objects.equals(o.getId(), organizerId));

        OrganizerProfile organizer = organizerProfileRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found"));

        organizer.getCoOrganizedTournaments().removeIf(t -> Objects.equals(t.getId(), tournamentId));
    }

    //TEAMS
    @Transactional
    public void registerTeamToTournament(TeamFormDto teamFormDto, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        validateTeamLimit(tournament, teamFormDto);

        Team team = teamMapper.toTeam(teamFormDto);
        team.setClub(commonService.findOrCreateEntity(teamFormDto.club(), Club.class, entityManager));
        team.setTournament(tournament);
        tournament.getTeams().add(team);

        registerTeamCreator(teamFormDto, tournament, team);

        registerInvitedParticipants(teamFormDto, tournament, team);
    }

    private void validateTeamLimit(Tournament tournament, TeamFormDto teamFormDto) {
        if(tournament.getTeams().size() + 1 >= tournament.getTeamLimit()) {
            throw new IllegalArgumentException("Team limit reached");
        }
    }

    private void registerTeamCreator(TeamFormDto teamFormDto, Tournament tournament, Team team) {
        ParticipantProfile teamCreator = entityManager.getReference(ParticipantProfile.class, teamFormDto.creatorId());

        TournamentParticipant participant = new TournamentParticipant();
        participant.setTeam(team);
        participant.setParticipantProfile(teamCreator);

        tournamentParticipantRepository.save(participant);
    }

    private void registerInvitedParticipants(TeamFormDto teamFormDto, Tournament tournament, Team team) {
        int requiredParticipants = (tournament.getPreliminaryFormat() == DebateFormat.APF) ? 1 : 2;

        if (teamFormDto.invitedParticipants().size() != requiredParticipants) {
            throw new IllegalArgumentException(
                    String.format("%s tournaments must have %d invited participant(s) per team",
                            tournament.getPreliminaryFormat(), requiredParticipants + 1)
            );
        }

        List<Long> invitedParticipantIds = teamFormDto.invitedParticipants().stream()
                .map(selector -> resolveParticipantProfile(selector, teamFormDto.creatorId()))
                .map(Profile::getId)
                .toList();

        participantInvitationService.createInvitations(teamFormDto.creatorId(), invitedParticipantIds, team.getId());

        team.setActive(false);
        team.setCheckedIn(false);

        teamRepository.save(team);
    }

    /**
     * Resolves a participant profile from a selector DTO.
     */
    private ParticipantProfile resolveParticipantProfile(ParticipantSelectorDto selector, Long creatorId) {
        Profile profile;

        if (selector.id() != null) {
            if (Objects.equals(creatorId, selector.id())) {
                throw new IllegalArgumentException("Team creator cannot be invited");
            }
            profile = entityManager.getReference(ParticipantProfile.class, selector.id());
        } else if (selector.username() != null) {
            profile = userService.loadUserByUsername(selector.username()).getProfile();
        } else {
            throw new IllegalArgumentException("Invalid participant selector");
        }

        if (!(profile instanceof ParticipantProfile participant)) {
            throw new IllegalArgumentException("Trying to register a non-participant profile");
        }

        return participant;
    }

    @Transactional
    public void removeTeamFromTournament(Long teamId, Long tournamentId) {
        tournamentRepository.removeTeamFromTournament(teamId, tournamentId);
    }

    //TOURNAMENT PROCESSES

    @Transactional
    public void checkInTeam(Long tournamentId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if(!Objects.equals(tournamentId, team.getTournament().getId())) {
            throw new IllegalArgumentException("Team does not belong to this tournament");
        }

        if(!team.getActive()) {
            throw new IllegalArgumentException("Not all members of the team has accepted the invitation");
        }

        int requiredParticipants = (team.getTournament().getPreliminaryFormat() == DebateFormat.KP) ? 3 : 2;

        if(team.getMembers().size() != requiredParticipants) {
            throw new IllegalArgumentException("Not enough team members");
        }

        team.setCheckedIn(true);
    }

    @Transactional
    public void uncheckInTeam(Long tournamentId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if(!Objects.equals(tournamentId, team.getTournament().getId())) {
            throw new IllegalArgumentException("Team does not belong to this tournament");
        }

        team.setCheckedIn(false);
    }

    /**
     * Starts a tournament. This method first checks if the tournament is valid to be started.
     * The checks are as follows:
     * <ul>
     *     <li>All teams must be checked in</li>
     *     <li>The tournament must have at least one judge</li>
     *     <li>If the first round is a BPF round, the number of teams must be divisible by 4</li>
     *     <li>The number of teams must be even</li>
     * </ul>
     * If any of these checks fail, an IllegalArgumentException is thrown.
     * Otherwise, the tournament is marked as started and the teams for the first round are set.
     * @param tournamentId The id of the tournament
     */
    @Transactional
    public void startTournament(Long tournamentId) {
        boolean error = false;
        StringBuilder errorMessage = new StringBuilder();

        TournamentCheckResult checkResult = tournamentRepository.checkTournament(tournamentId);

        if(checkResult.getUncheckedIn() > 0) {
            error = true;
            errorMessage.append("Not all teams are not checked in\n");;
        }

        if(checkResult.getJudgeCount() == 0) {
            error = true;
            errorMessage.append("Tournament has no judges\n");
        }

        Round firstRound = tournamentRepository.findRound(tournamentId, RoundGroupType.PRELIMINARY, 1);

        DebateFormat format = firstRound.getCustomFormat() == null ? firstRound.getRoundGroup().getFormat() : firstRound.getCustomFormat();
        int teamCount = checkResult.getTeamCount();

        if(format == DebateFormat.BPF && teamCount % 4 != 0) {
            error = true;
            errorMessage.append("BPF rounds must have a number of teams divisible by 4\n");
        } else if(teamCount % 2 != 0) {
            error = true;
            errorMessage.append("Tournament must have an even number of teams\n");
        }

        if(error) {
            throw new IllegalArgumentException(errorMessage.toString());
        }

        firstRound.getRoundGroup().getTournament().setStarted(true);

        setTeamsOfFirstRound(firstRound);
    }

    private void setTeamsOfFirstRound(Round round) {
        round.setTeams(round.getRoundGroup().getTournament().getTeams());
    }

    //TOURNAMENT DISABLING & DELETING

    @Transactional
    public void disableTournament(Long id) {
        tournamentRepository.disableById(id);
    }

    @Transactional
    public void enableTournament(Long id) {
        tournamentRepository.enableById(id);
    }

    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
        fileService.deleteFile(tournament.getImageUrl());
        tournamentRepository.delete(tournament);
    }
}