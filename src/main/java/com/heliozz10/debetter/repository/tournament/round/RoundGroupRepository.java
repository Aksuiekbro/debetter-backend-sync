package com.heliozz10.debetter.repository.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundGroupRepository extends JpaRepository<RoundGroup, Long> {
    List<RoundGroup> findByTournamentId(Long tournamentId);

    //THIS SHOULD BE A LIST OF ROUNDS GROUPS, HOWEVER THE COMPOSITE KEY IN THE ROUND GROUP ENTITY IS NOT IMPLEMENTED
    Optional<RoundGroup> findByTypeAndTournamentId(RoundGroupType roundGroupType, Long tournamentId);

    @Modifying
    @Query("UPDATE RoundGroup r SET r.format = :format WHERE r.type = :roundGroupType AND r.tournament.id = :tournamentId")
    void changeRoundGroupFormat(RoundGroupType roundGroupType, DebateFormat format, Long tournamentId);

    Optional<RoundGroup> findByTournamentIdAndId(Long tournamentId, Long id);
}
