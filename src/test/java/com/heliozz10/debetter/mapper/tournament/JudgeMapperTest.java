package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeMapperTest {
    private final JudgeMapper judgeMapper = Mappers.getMapper(JudgeMapper.class);

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
}
