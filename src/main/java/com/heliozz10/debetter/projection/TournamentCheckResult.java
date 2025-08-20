package com.heliozz10.debetter.projection;

import com.heliozz10.debetter.content.tournament.DebateFormat;

public interface TournamentCheckResult {
    int getUncheckedIn();
    int getJudgeCount();
    int getTeamCount();
}
