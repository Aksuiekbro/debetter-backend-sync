package com.heliozz10.debetter.repository.user.profile;

import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantProfileRepository extends JpaRepository<ParticipantProfile, Long> {
    List<ParticipantProfile> findByCityId(Long cityId);
    List<ParticipantProfile> findByInstitutionId(Long institutionId);

    List<ParticipantProfile> findByRatingBetween(Integer minRating, Integer maxRating);

    Optional<ParticipantProfile> findByUser_Username(String username);
}
