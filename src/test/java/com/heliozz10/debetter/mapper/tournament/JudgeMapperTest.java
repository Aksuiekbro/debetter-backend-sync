package com.heliozz10.debetter.mapper.tournament;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.util.socials.SocialProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeMapperTest {
    private final JudgeMapper judgeMapper = Mappers.getMapper(JudgeMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                judgeMapper,
                "socialProfileMapper",
                Mappers.getMapper(SocialProfileMapper.class)
        );
    }

    @Test
    void updateJudgeKeepsExistingFieldsWhenPatchDtoOnlyChangesCheckIn() {
        Judge judge = new Judge();
        judge.setFullName("Aigerim Judge");
        judge.setEmail("judge@example.com");
        judge.setPhoneNumber("+77010000000");
        judge.setCheckedIn(false);

        judgeMapper.updateJudge(new JudgeFormDto(null, null, null, true), judge);

        assertEquals("Aigerim Judge", judge.getFullName());
        assertEquals("judge@example.com", judge.getEmail());
        assertEquals("+77010000000", judge.getPhoneNumber());
        assertTrue(judge.getCheckedIn());
    }

    @Test
    void organizerViewRetainsJudgeContactDetails() throws Exception {
        JudgeView view = judgeMapper.toOrganizerJudgeView(judgeWithContactDetails());
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(view));

        assertEquals("judge@example.com", view.getEmail());
        assertEquals("+77010000000", view.getPhoneNumber());
        assertEquals("judge@example.com", json.get("email").asText());
        assertEquals("+77010000000", json.get("phoneNumber").asText());
    }

    @Test
    void publicViewNeverCopiesOrSerializesJudgeContactDetails() throws Exception {
        JudgeView view = judgeMapper.toPublicJudgeView(judgeWithContactDetails());
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(view));

        assertNull(view.getEmail());
        assertNull(view.getPhoneNumber());
        assertFalse(json.has("email"));
        assertFalse(json.has("phoneNumber"));
        assertEquals("Aigerim Judge", json.get("fullName").asText());
        assertFalse(json.get("checkedIn").asBoolean());
    }

    private Judge judgeWithContactDetails() {
        Judge judge = new Judge();
        judge.setId(12L);
        judge.setFullName("Aigerim Judge");
        judge.setEmail("judge@example.com");
        judge.setPhoneNumber("+77010000000");
        judge.setCheckedIn(false);
        return judge;
    }
}
