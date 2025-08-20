package com.heliozz10.debetter.repository.util.socials;

import com.heliozz10.debetter.content.util.socials.SocialProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialProfileRepository extends JpaRepository<SocialProfile, Long> {
}
