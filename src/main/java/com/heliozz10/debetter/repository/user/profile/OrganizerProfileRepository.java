package com.heliozz10.debetter.repository.user.profile;

import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerProfileRepository extends JpaRepository<OrganizerProfile, Long> {
    @EntityGraph(value = "OrganizerProfile.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Optional<OrganizerProfile> findById(Long id);

    @EntityGraph(value = "OrganizerProfile.withAnnouncements", type = EntityGraph.EntityGraphType.LOAD)
    Optional<OrganizerProfile> findWithAnnouncementsById(Long id);

    @EntityGraph(value = "Profile.withUser", type = EntityGraph.EntityGraphType.LOAD)
    Optional<OrganizerProfile> findWithUserById(Long id);

    Optional<OrganizerProfile> findByUser_Username(String username);
}
