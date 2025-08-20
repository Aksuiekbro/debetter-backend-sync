package com.heliozz10.debetter.service.user.profile;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrganizerProfileService implements ProfileService {
    private final EntityManager entityManager;

    private final OrganizerProfileRepository organizerProfileRepository;

    @Transactional
    @Override
    public OrganizerProfile createProfile(Long userId) {
        OrganizerProfile profile = new OrganizerProfile();
        User user = entityManager.getReference(User.class, userId);
        profile.setUser(user);
        return organizerProfileRepository.save(profile);
    }
}
