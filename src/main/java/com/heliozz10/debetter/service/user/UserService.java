package com.heliozz10.debetter.service.user;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.City;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.profile.institution.Institution;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.content.util.socials.SocialPlatform;
import com.heliozz10.debetter.content.util.socials.SocialProfile;
import com.heliozz10.debetter.dto.user.in.UserGetParams;
import com.heliozz10.debetter.dto.user.in.UserRegistrationDto;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.dto.util.socials.in.SocialProfileDto;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.specification.user.UserSpecification;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.CityRepository;
import com.heliozz10.debetter.repository.user.profile.institution.InstitutionRepository;
import com.heliozz10.debetter.service.CommonService;
import com.heliozz10.debetter.service.user.profile.OrganizerProfileService;
import com.heliozz10.debetter.service.user.profile.ParticipantProfileService;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

//TODO: create remaining user methods in the controller, finish validation, add file != null check everywhere needed
@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final EntityManager entityManager;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final ParticipantProfileService participantProfileService;
    private final OrganizerProfileService organizerProfileService;

    private final CityRepository cityRepository;
    private final InstitutionRepository institutionRepository;

    private final CommonService commonService;
    private final FileService fileService;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> getUsers(UserGetParams params, Pageable pageable) {
        Specification<User> spec = UserSpecification.filterBy(params, entityManager);
        return userRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public User createUser(UserRegistrationDto dto) {
        if (userRepository.existsByUsernameOrEmail(dto.username(), dto.email())) {
            throw new DataIntegrityViolationException("Username or email already exists");
        }

        User user = userMapper.toUser(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        if(user.getRole() == Role.PARTICIPANT) {
            participantProfileService.createProfile(user.getId(), dto.city(), dto.institution());
        } else if(user.getRole() == Role.ORGANIZER) {
            organizerProfileService.createProfile(user.getId());
        }
        return user;
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public User updateUser(UserUpdateDto dto, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if(user.getRole() == Role.PARTICIPANT) {
            ParticipantProfile profile = ((ParticipantProfile) user.getProfile());
            profile.setCity(commonService.findOrCreateEntity(dto.city().name(), City.class, entityManager));
            profile.setInstitution(commonService.findOrCreateEntity(dto.institution().name(), Institution.class, entityManager));
        }
        boolean error = false;
        StringBuilder errorMessage = new StringBuilder();
        if(passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.newPassword()));
        }
        userMapper.updateUser(dto, user);
        return user;
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public void addOrUpdateProfilePicture(Long userId, MultipartFile file) {
        Url url = fileService.uploadImage(file, "profile-pictures", UUID.randomUUID().toString());
        if(userRepository.updateImageUrl(userId, url) == 0) {
            fileService.deleteFile(url);
            throw new EntityNotFoundException("User not found");
        }
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public void deleteProfilePicture(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        fileService.deleteFile(user.getImageUrl());
        user.setImageUrl(null);
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public void addOrUpdateSocialProfiles(Long userId, Collection<SocialProfileDto> newProfiles) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        addOrUpdateSocialProfiles(user, newProfiles);
    }

    @Transactional
    public void addOrUpdateSocialProfiles(User user, Collection<SocialProfileDto> newProfiles) {
        Map<SocialPlatform, SocialProfile> currentProfiles = user.getSocialProfiles()
                .stream()
                .collect(Collectors.toMap(SocialProfile::getSocialPlatform, sp -> sp));

        if(newProfiles == null) {
            return;
        }

        for (SocialProfileDto dto : newProfiles) {
            SocialProfile existing = currentProfiles.get(dto.socialPlatform());

            if (existing != null) {
                existing.setHandle(dto.handle());
                existing.setIsPublic(dto.isPublic() != null ? dto.isPublic() : true);
            } else {
                SocialProfile newProfile = new SocialProfile();
                newProfile.setSocialPlatform(dto.socialPlatform());
                newProfile.setHandle(dto.handle());
                newProfile.setIsPublic(dto.isPublic() != null ? dto.isPublic() : true);
                user.getSocialProfiles().add(newProfile);
            }
        }
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public void removeAllSocialProfiles(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.getSocialProfiles().clear();
    }

    @CacheEvict(value = "currentUser", key = "#userId")
    @Transactional
    public void removeSocialProfiles(Long userId, Collection<SocialPlatform> platforms) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        removeSocialProfiles(user, platforms);
    }

    @Transactional
    public void removeSocialProfiles(User user, Collection<SocialPlatform> platforms) {
        for (SocialPlatform platform : platforms) {
            user.getSocialProfiles().removeIf(sp -> sp.getSocialPlatform() == platform);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findForSecurityByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
