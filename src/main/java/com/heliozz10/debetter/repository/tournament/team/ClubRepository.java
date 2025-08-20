package com.heliozz10.debetter.repository.tournament.team;

import com.heliozz10.debetter.content.tournament.team.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

}
