package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchParticipantScoreRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchParticipantScoreReadTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantScoreRepository matchParticipantScoreRepository;

    @Autowired
    private JudgeRepository judgeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTournamentRoleRepository userTournamentRoleRepository;

    @Autowired
    private ParticipantProfileRepository participantProfileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void anonymousMatchGetRedactsBpfResultsButPreservesPairingAndCompletionMetadata() throws Exception {
        TeamFixture fixture = bpfFixture();

        mockMvc.perform(get(fixture.endpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].team1.id").value(fixture.teams().get(0).getId()))
                .andExpect(jsonPath("$.content[0].team2.id").value(fixture.teams().get(1).getId()))
                .andExpect(jsonPath("$.content[0].team3.id").value(fixture.teams().get(2).getId()))
                .andExpect(jsonPath("$.content[0].team4.id").value(fixture.teams().get(3).getId()))
                .andExpect(jsonPath("$.content[0].team1Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team2Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team3Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team4Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team1Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team2Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team3Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team4Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].participantScoresComplete").value(true))
                .andExpect(jsonPath("$.content[0].participantScoresRepairable").value(false))
                .andExpect(jsonPath("$.content[0].team1ParticipantScores").doesNotExist())
                .andExpect(jsonPath("$.content[0].team4ParticipantScores").doesNotExist())
                .andExpect(jsonPath("$.content[0].judge.fullName").value("Judge Privacy"))
                .andExpect(jsonPath("$.content[0].judge.phoneNumber").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.email").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.socialProfiles").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.checkedIn").value(nullValue()));
    }

    @Test
    void ordinaryParticipantMatchGetUsesTheSameRedactedBpfResultShape() throws Exception {
        TeamFixture fixture = bpfFixture();
        User participantUser = user("participant-read-" + UUID.randomUUID(), Role.PARTICIPANT);

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(new UsernamePasswordAuthenticationToken(participantUser, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].team1.id").value(fixture.teams().getFirst().getId()))
                .andExpect(jsonPath("$.content[0].team1Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team4Score").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team1Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].team4Won").value(nullValue()))
                .andExpect(jsonPath("$.content[0].participantScoresComplete").value(true))
                .andExpect(jsonPath("$.content[0].participantScoresRepairable").value(false))
                .andExpect(jsonPath("$.content[0].team1ParticipantScores").doesNotExist())
                .andExpect(jsonPath("$.content[0].team4ParticipantScores").doesNotExist())
                .andExpect(jsonPath("$.content[0].judge.email").value(nullValue()));
    }

    @Test
    void authorizedOrganizerMatchGetReceivesExactBpfResultsAndParticipantScores() throws Exception {
        TeamFixture fixture = bpfFixture();
        UsernamePasswordAuthenticationToken organizer = grantFullAccess(fixture.tournament());

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                .with(authentication(organizer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].participantScoresComplete").value(true))
                .andExpect(jsonPath("$.content[0].participantScoresRepairable").value(false))
                .andExpect(jsonPath("$.content[0].team1Score").value(141))
                .andExpect(jsonPath("$.content[0].team2Score").value(145))
                .andExpect(jsonPath("$.content[0].team3Score").value(149))
                .andExpect(jsonPath("$.content[0].team4Score").value(153))
                .andExpect(jsonPath("$.content[0].team1Won").value(true))
                .andExpect(jsonPath("$.content[0].team2Won").value(false))
                .andExpect(jsonPath("$.content[0].team3Won").value(true))
                .andExpect(jsonPath("$.content[0].team4Won").value(false))
                .andExpect(jsonPath("$.content[0].team1ParticipantScores[*].participantId")
                        .value(containsInAnyOrder(
                                fixture.participants().get(0).getId().intValue(),
                                fixture.participants().get(1).getId().intValue()
                        )))
                .andExpect(jsonPath("$.content[0].team1ParticipantScores[*].score")
                        .value(containsInAnyOrder(70, 71)))
                .andExpect(jsonPath("$.content[0].team2ParticipantScores[*].participantId")
                        .value(containsInAnyOrder(
                                fixture.participants().get(2).getId().intValue(),
                                fixture.participants().get(3).getId().intValue()
                        )))
                .andExpect(jsonPath("$.content[0].team2ParticipantScores[*].score")
                        .value(containsInAnyOrder(72, 73)))
                .andExpect(jsonPath("$.content[0].team3ParticipantScores[*].score")
                        .value(containsInAnyOrder(74, 75)))
                .andExpect(jsonPath("$.content[0].team4ParticipantScores[*].score")
                        .value(containsInAnyOrder(76, 77)))
                .andExpect(jsonPath("$.content[0].judge.phoneNumber").value("+77010000000"))
                .andExpect(jsonPath("$.content[0].judge.email").value("judge@example.invalid"))
                .andExpect(jsonPath("$.content[0].judge.checkedIn").value(true));
    }

    @Test
    void anonymousAndOrganizerLdReadsUseDifferentScoreShapes() throws Exception {
        LdFixture fixture = ldFixture();

        mockMvc.perform(get(fixture.endpoint()).servletPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].debater1.id").value(fixture.debater1().getId()))
                .andExpect(jsonPath("$.content[0].debater2.id").value(fixture.debater2().getId()))
                .andExpect(jsonPath("$.content[0].debater1Score").doesNotExist())
                .andExpect(jsonPath("$.content[0].debater2Score").doesNotExist())
                .andExpect(jsonPath("$.content[0].debater1.speakerScore").doesNotExist())
                .andExpect(jsonPath("$.content[0].debater2.speakerScore").doesNotExist())
                .andExpect(jsonPath("$.content[0].debater1.participantProfile").value(nullValue()))
                .andExpect(jsonPath("$.content[0].debater2.participantProfile").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.fullName").value("LD Judge Privacy"))
                .andExpect(jsonPath("$.content[0].judge.phoneNumber").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.email").value(nullValue()))
                .andExpect(jsonPath("$.content[0].judge.checkedIn").value(nullValue()));

        mockMvc.perform(get(fixture.endpoint())
                        .servletPath("/api")
                        .with(authentication(grantFullAccess(fixture.tournament()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].debater1Score").value(80))
                .andExpect(jsonPath("$.content[0].debater2Score").value(70))
                .andExpect(jsonPath("$.content[0].debater1.speakerScore").value(17))
                .andExpect(jsonPath("$.content[0].debater2.speakerScore").value(23))
                .andExpect(jsonPath("$.content[0].debater1.participantProfile.rating").value(901))
                .andExpect(jsonPath("$.content[0].debater2.participantProfile.rating").value(902))
                .andExpect(jsonPath("$.content[0].judge.phoneNumber").value("+77020000000"))
                .andExpect(jsonPath("$.content[0].judge.email").value("ld-judge@example.invalid"))
                .andExpect(jsonPath("$.content[0].judge.checkedIn").value(true));
    }

    @Test
    void unauthorizedResultWritesRemain401Or403() throws Exception {
        TeamFixture fixture = bpfFixture();
        String resultsEndpoint = fixture.endpoint() + "/results";

        mockMvc.perform(patch(resultsEndpoint)
                        .servletPath("/api")
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isForbidden());

        User participantUser = user("participant-write-" + UUID.randomUUID(), Role.PARTICIPANT);
        mockMvc.perform(patch(resultsEndpoint)
                        .servletPath("/api")
                        .with(authentication(new UsernamePasswordAuthenticationToken(participantUser, null, List.of())))
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    private TeamFixture bpfFixture() {
        Tournament tournament = tournament();
        tournament.setPreliminaryFormat(DebateFormat.BPF);
        tournament.setTeamEliminationFormat(DebateFormat.BPF);
        tournament = tournamentRepository.save(tournament);
        RoundGroup roundGroup = roundGroupRepository.save(new RoundGroup(tournament, RoundGroupType.PRELIMINARY, DebateFormat.BPF));
        roundGroup.setCurrentRoundNumber(1);

        Round round = new Round(roundGroup, "Round 1", 1);
        round.setMatchesArePublic(true);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        Team team1 = teamRepository.save(team(tournament, "Affirmative"));
        Team team2 = teamRepository.save(team(tournament, "Negative"));
        Team team3 = teamRepository.save(team(tournament, "Opening Opposition"));
        Team team4 = teamRepository.save(team(tournament, "Closing Opposition"));
        TournamentParticipant participant1 = tournamentParticipantRepository.save(participant(team1));
        TournamentParticipant participant2 = tournamentParticipantRepository.save(participant(team1));
        TournamentParticipant participant3 = tournamentParticipantRepository.save(participant(team2));
        TournamentParticipant participant4 = tournamentParticipantRepository.save(participant(team2));
        TournamentParticipant participant5 = tournamentParticipantRepository.save(participant(team3));
        TournamentParticipant participant6 = tournamentParticipantRepository.save(participant(team3));
        TournamentParticipant participant7 = tournamentParticipantRepository.save(participant(team4));
        TournamentParticipant participant8 = tournamentParticipantRepository.save(participant(team4));
        Judge judge = judgeRepository.save(judge(tournament, "Judge Privacy", "+77010000000", "judge@example.invalid"));

        Match match = new Match();
        match.setRound(round);
        match.setTeam1(team1);
        match.setTeam2(team2);
        match.setTeam3(team3);
        match.setTeam4(team4);
        match.setJudge(judge);
        match.setTeam1Score(141);
        match.setTeam2Score(145);
        match.setTeam3Score(149);
        match.setTeam4Score(153);
        match.setTeam1Won(true);
        match.setTeam2Won(false);
        match.setTeam3Won(true);
        match.setTeam4Won(false);
        match.setCompleted(true);
        match.setIsBye(false);
        match = matchRepository.saveAndFlush(match);

        matchParticipantScoreRepository.saveAndFlush(score(match, participant1, 70));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant2, 71));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant3, 72));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant4, 73));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant5, 74));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant6, 75));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant7, 76));
        matchParticipantScoreRepository.saveAndFlush(score(match, participant8, 77));
        entityManager.clear();

        return new TeamFixture(
                "/api/tournaments/" + tournament.getId()
                        + "/round-groups/" + roundGroup.getId()
                        + "/rounds/" + round.getId() + "/matches",
                tournament,
                List.of(team1, team2, team3, team4),
                List.of(participant1, participant2, participant3, participant4,
                        participant5, participant6, participant7, participant8)
        );
    }

    private LdFixture ldFixture() {
        Tournament tournament = tournamentRepository.save(tournament());
        RoundGroup roundGroup = roundGroupRepository.save(new RoundGroup(tournament, RoundGroupType.SOLO_ELIMINATION, DebateFormat.LD));
        Round round = new Round(roundGroup, "LD Round", 1);
        round.setMatchesArePublic(true);
        round.setTeams(new ArrayList<>());
        round.setDebaters(new ArrayList<>());
        round.setMatches(new ArrayList<>());
        round = roundRepository.saveAndFlush(round);

        TournamentParticipant debater1 = participant(null);
        debater1.setSpeakerScore(17);
        debater1.setParticipantProfile(participantProfile("ld-debater-1-" + UUID.randomUUID(), 901));
        debater1 = tournamentParticipantRepository.save(debater1);
        TournamentParticipant debater2 = participant(null);
        debater2.setSpeakerScore(23);
        debater2.setParticipantProfile(participantProfile("ld-debater-2-" + UUID.randomUUID(), 902));
        debater2 = tournamentParticipantRepository.save(debater2);
        Judge judge = judgeRepository.save(judge(tournament, "LD Judge Privacy", "+77020000000", "ld-judge@example.invalid"));
        Match match = new Match();
        match.setRound(round);
        match.setDebater1(debater1);
        match.setDebater2(debater2);
        match.setJudge(judge);
        match.setDebater1Score(80);
        match.setDebater2Score(70);
        match.setCompleted(true);
        match.setIsBye(false);
        matchRepository.saveAndFlush(match);
        entityManager.clear();

        return new LdFixture(
                "/api/tournaments/" + tournament.getId()
                        + "/round-groups/" + roundGroup.getId()
                        + "/rounds/" + round.getId() + "/matches",
                tournament,
                debater1,
                debater2
        );
    }

    private UsernamePasswordAuthenticationToken grantFullAccess(Tournament tournament) {
        User organizer = user("organizer-read-" + UUID.randomUUID(), Role.ORGANIZER);
        UserTournamentRole role = new UserTournamentRole();
        role.setId(new UserTournamentKey(organizer.getId(), tournament.getId()));
        role.setUser(organizer);
        role.setTournament(tournamentRepository.getReferenceById(tournament.getId()));
        role.setRole(TournamentRole.FULL);
        userTournamentRoleRepository.saveAndFlush(role);
        return new UsernamePasswordAuthenticationToken(organizer, null, List.of());
    }

    private User user(String username, Role role) {
        return userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "User",
                role
        ));
    }

    private ParticipantProfile participantProfile(String username, int rating) {
        User account = user(username, Role.PARTICIPANT);
        ParticipantProfile profile = new ParticipantProfile();
        profile.setUser(account);
        profile.setRating(rating);
        profile = participantProfileRepository.saveAndFlush(profile);
        account.setProfile(profile);
        return profile;
    }

    private record TeamFixture(
            String endpoint,
            Tournament tournament,
            List<Team> teams,
            List<TournamentParticipant> participants
    ) {
    }

    private record LdFixture(
            String endpoint,
            Tournament tournament,
            TournamentParticipant debater1,
            TournamentParticipant debater2
    ) {
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Participant score GET contract");
        tournament.setDescription("Regression fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(8);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }

    private static Team team(Tournament tournament, String name) {
        Team team = new Team();
        team.setName(name);
        team.setTournament(tournament);
        team.setActive(true);
        team.setCheckedIn(true);
        team.setDisqualified(false);
        return team;
    }

    private static Judge judge(Tournament tournament, String fullName, String phoneNumber, String email) {
        Judge judge = new Judge();
        judge.setTournament(tournament);
        judge.setFullName(fullName);
        judge.setPhoneNumber(phoneNumber);
        judge.setEmail(email);
        judge.setTimesJudged(0);
        judge.setCheckedIn(true);
        return judge;
    }

    private static TournamentParticipant participant(Team team) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setTeam(team);
        participant.setSpeakerScore(0);
        return participant;
    }

    private static MatchParticipantScore score(Match match, TournamentParticipant participant, int score) {
        MatchParticipantScore participantScore = new MatchParticipantScore();
        participantScore.setMatch(match);
        participantScore.setParticipant(participant);
        participantScore.setScore(score);
        return participantScore;
    }
}
