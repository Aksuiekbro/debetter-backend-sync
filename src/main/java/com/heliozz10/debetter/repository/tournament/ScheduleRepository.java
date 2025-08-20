package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByTournamentId(Long tournamentId);

    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.tournament = NULL WHERE s.id = :scheduleId AND s.tournament.id = :tournamentId")
    void removeScheduleFromTournament(@Param("scheduleId") Long scheduleId,
                                      @Param("tournamentId") Long tournamentId);

    Optional<Schedule> findByTournamentIdAndId(Long tournamentId, Long id);
}
