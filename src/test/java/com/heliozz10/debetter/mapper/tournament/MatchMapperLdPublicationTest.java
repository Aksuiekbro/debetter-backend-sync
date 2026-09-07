package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.dto.tournament.match.out.MatchView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchMapperLdPublicationTest {
    private final MatchMapper mapper = Mappers.getMapper(MatchMapper.class);

    @BeforeEach
    void setUp() {
        TournamentParticipantMapper participantMapper = Mappers.getMapper(TournamentParticipantMapper.class);
        ReflectionTestUtils.setField(participantMapper, "userMapper", Mappers.getMapper(UserMapper.class));
        ReflectionTestUtils.setField(participantMapper, "participantProfileMapper",
                Mappers.getMapper(ParticipantProfileMapper.class));
        ReflectionTestUtils.setField(mapper, "tournamentParticipantMapper", participantMapper);
        ReflectionTestUtils.setField(mapper, "judgeMapper", Mappers.getMapper(JudgeMapper.class));
    }

    @ParameterizedTest
    @CsvSource({"80,70,101", "70,80,102"})
    void publishedPreliminaryLdIdentifiesWinnerWithoutExposingScoresOrMutatingBallot(
            int firstScore, int secondScore, long expectedWinner
    ) {
        Match match = preliminaryLdMatch();
        match.setDebater1Score(firstScore);
        match.setDebater2Score(secondScore);

        MatchView view = mapper.toMatchView(match, false, true);

        assertEquals(expectedWinner, view.getWinnerParticipantId());
        assertTrue(view.getCompleted());
        assertScoresRedacted(view);
        assertNull(match.getWinnerParticipantId());
        assertEquals(firstScore, match.getDebater1Score());
        assertEquals(secondScore, match.getDebater2Score());
        assertEquals(150, match.getDebater1().getSpeakerScore());
        assertEquals(140, match.getDebater2().getSpeakerScore());
    }

    @Test
    void hiddenPreliminaryLdDoesNotExposeWinnerOrScores() {
        MatchView view = mapper.toMatchView(preliminaryLdMatch(), false, false);

        assertNull(view.getWinnerParticipantId());
        assertScoresRedacted(view);
    }

    @Test
    void exactEditorViewKeepsScoresAndDoesNotSynthesizeWinner() {
        MatchView view = mapper.toMatchView(preliminaryLdMatch(), true, true);

        assertNull(view.getWinnerParticipantId());
        assertEquals(80, view.getDebater1Score());
        assertEquals(70, view.getDebater2Score());
        assertEquals(150, view.getDebater1().getSpeakerScore());
        assertEquals(140, view.getDebater2().getSpeakerScore());
    }

    @ParameterizedTest
    @CsvSource(value = {"false,80,70", "null,80,70", "true,null,70", "true,80,null",
            "true,80,80", "true,-1,70", "true,80,-1"}, nullValues = "null")
    void pendingOrInvalidPreliminaryLdDoesNotProduceAWinner(Boolean completed, Integer first, Integer second) {
        Match match = preliminaryLdMatch();
        match.setCompleted(completed);
        match.setDebater1Score(first);
        match.setDebater2Score(second);

        MatchView view = mapper.toMatchView(match, false, true);

        assertNull(view.getWinnerParticipantId());
        assertScoresRedacted(view);
    }

    @ParameterizedTest
    @CsvSource(value = {"null,102", "101,null", "101,101"}, nullValues = "null")
    void missingOrDuplicateParticipantIdsDoNotProduceAWinner(Long firstId, Long secondId) {
        Match match = preliminaryLdMatch();
        match.getDebater1().setId(firstId);
        match.getDebater2().setId(secondId);

        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void missingDebaterDoesNotProduceAWinner(boolean missingFirst) {
        Match match = preliminaryLdMatch();
        if (missingFirst) {
            match.setDebater1(null);
        } else {
            match.setDebater2(null);
        }

        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
    }

    @ParameterizedTest
    @EnumSource(value = DebateFormat.class, names = "LD", mode = EnumSource.Mode.EXCLUDE)
    void teamFormatsDoNotDeriveAnIndividualWinner(DebateFormat format) {
        Match match = preliminaryLdMatch();
        match.getRound().getRoundGroup().setFormat(format);

        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
    }

    @Test
    void derivationUsesTheRoundsCustomFormat() {
        Match match = preliminaryLdMatch();
        match.getRound().getRoundGroup().setFormat(DebateFormat.APF);
        match.getRound().setCustomFormat(DebateFormat.LD);

        assertEquals(101L, mapper.toMatchView(match, false, true).getWinnerParticipantId());

        match.getRound().getRoundGroup().setFormat(DebateFormat.LD);
        match.getRound().setCustomFormat(DebateFormat.APF);
        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
    }

    @Test
    void soloEliminationKeepsItsRecordedWinnerAndStillRespectsPublication() {
        Match match = preliminaryLdMatch();
        match.getRound().getRoundGroup().setType(RoundGroupType.SOLO_ELIMINATION);
        match.setDebater1Score(null);
        match.setDebater2Score(null);
        match.setWinnerParticipantId(102L);

        assertEquals(102L, mapper.toMatchView(match, false, true).getWinnerParticipantId());
        assertNull(mapper.toMatchView(match, false, false).getWinnerParticipantId());
        assertEquals(102L, mapper.toMatchView(match, true, true).getWinnerParticipantId());
        assertEquals(102L, match.getWinnerParticipantId());
    }

    @Test
    void missingFormatOrRoundDoesNotProduceAWinner() {
        Match match = preliminaryLdMatch();
        match.getRound().getRoundGroup().setFormat(null);
        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
        match.getRound().setRoundGroup(null);
        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
        match.setRound(null);
        assertNull(mapper.toMatchView(match, false, true).getWinnerParticipantId());
    }

    private static Match preliminaryLdMatch() {
        RoundGroup group = new RoundGroup(null, RoundGroupType.PRELIMINARY, DebateFormat.LD);
        Round round = new Round();
        round.setRoundGroup(group);
        Match match = new Match();
        match.setRound(round);
        match.setCompleted(true);
        match.setDebater1(participant(101L, 150));
        match.setDebater2(participant(102L, 140));
        match.setDebater1Score(80);
        match.setDebater2Score(70);
        return match;
    }

    private static TournamentParticipant participant(long id, int score) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setId(id);
        participant.setSpeakerScore(score);
        return participant;
    }

    private static void assertScoresRedacted(MatchView view) {
        assertNull(view.getDebater1Score());
        assertNull(view.getDebater2Score());
        assertNull(view.getDebater1().getSpeakerScore());
        assertNull(view.getDebater2().getSpeakerScore());
        assertNull(view.getTeam1ParticipantScores());
        assertNull(view.getTeam2ParticipantScores());
        assertNull(view.getTeam3ParticipantScores());
        assertNull(view.getTeam4ParticipantScores());
    }
}
