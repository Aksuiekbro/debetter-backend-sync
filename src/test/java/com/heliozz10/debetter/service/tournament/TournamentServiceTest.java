package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.dto.tournament.team.in.TeamFormDto;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.mapper.tournament.TournamentMapper;
import com.heliozz10.debetter.mapper.tournament.announcement.AnnouncementMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
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
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import com.heliozz10.debetter.service.CommonService;
import com.heliozz10.debetter.service.tournament.round.RoundService;
import com.heliozz10.debetter.service.user.UserService;
import com.heliozz10.debetter.service.util.media.FileService;
import com.heliozz10.debetter.service.util.request.ParticipantInvitationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {
    @Mock
    private EntityManager entityManager;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentMapper tournamentMapper;
    @Mock
    private TournamentSecurity tournamentSecurity;
    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private JudgeRepository judgeRepository;
    @Mock
    private JudgeMapper judgeMapper;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementMapper announcementMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrganizerProfileRepository organizerProfileRepository;
    @Mock
    private ParticipantInvitationService participantInvitationService;
    @Mock
    private RoundService roundService;
    @Mock
    private MatchService matchService;
    @Mock
    private FileService fileService;
    @Mock
    private CommonService commonService;
    @Mock
    private ParticipantProfileRepository participantProfileRepository;
    @Mock
    private RoundGroupRepository roundGroupRepository;
    @Mock
    private RoundRepository roundRepository;

    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        tournamentService = new TournamentService(
                entityManager,
                tournamentRepository,
                tournamentMapper,
                tournamentSecurity,
                tournamentParticipantRepository,
                teamRepository,
                teamService,
                teamMapper,
                judgeRepository,
                judgeMapper,
                announcementRepository,
                announcementMapper,
                userRepository,
                userService,
                userMapper,
                organizerProfileRepository,
                participantInvitationService,
                roundService,
                matchService,
                fileService,
                commonService,
                participantProfileRepository,
                roundGroupRepository,
                roundRepository
        );
    }

    @Test
    void registerTeamRejectsCreatorAlreadyRegisteredInTournament() {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamLimit(32);
        tournament.setTeams(new ArrayList<>());

        TeamFormDto dto = new TeamFormDto("Team A", "Club A", null, List.of());

        when(tournamentRepository.findWithTeamsById(53L)).thenReturn(Optional.of(tournament));
        when(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(53L, 10L))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tournamentService.registerTeamToTournament(dto, 53L, 10L)
        );

        assertEquals("You are already registered for this tournament.", exception.getMessage());
        verify(teamRepository, never()).save(any());
        verify(tournamentSecurity, never()).assignRoleToUser(any(), any(), any());
    }

    @Test
    void registerTeamAllowsTeamThatExactlyReachesLimit() {
        Tournament tournament = buildTournamentWithExistingTeamCount(31);
        TeamFormDto dto = new TeamFormDto("Team 32", "NIS", null, List.of());
        Team team = new Team();
        Club club = new Club();
        ParticipantProfile creator = new ParticipantProfile();
        creator.setId(2000L);
        User user = new User();
        user.setId(3000L);
        creator.setUser(user);

        when(tournamentRepository.findWithTeamsById(53L)).thenReturn(Optional.of(tournament));
        when(tournamentParticipantRepository.existsByTeam_Tournament_IdAndParticipantProfile_Id(53L, 2000L))
                .thenReturn(false);
        when(teamMapper.toTeam(dto)).thenReturn(team);
        when(commonService.findOrCreateEntity("NIS", Club.class, entityManager)).thenReturn(club);
        when(teamRepository.save(team)).thenReturn(team);
        when(participantProfileRepository.findById(2000L)).thenReturn(Optional.of(creator));

        assertDoesNotThrow(() -> tournamentService.registerTeamToTournament(dto, 53L, 2000L));

        verify(teamRepository, times(2)).save(team);
        verify(tournamentSecurity).assignRoleToUser(3000L, 53L, com.heliozz10.debetter.content.user.role.TournamentRole.VIEW);
    }

    @Test
    void registerTeamRejectsTeamAfterLimitIsReached() {
        Tournament tournament = buildTournamentWithExistingTeamCount(32);
        TeamFormDto dto = new TeamFormDto("Team 33", "NIS", null, List.of());

        when(tournamentRepository.findWithTeamsById(53L)).thenReturn(Optional.of(tournament));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tournamentService.registerTeamToTournament(dto, 53L, 2000L)
        );

        assertEquals("Team limit reached", exception.getMessage());
        verify(teamRepository, never()).save(any());
        verify(tournamentSecurity, never()).assignRoleToUser(any(), any(), any());
    }

    private Tournament buildTournamentWithExistingTeamCount(int teamCount) {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamLimit(32);
        tournament.setTeams(IntStream.range(0, teamCount).mapToObj(i -> new Team()).toList());
        return tournament;
    }
}
