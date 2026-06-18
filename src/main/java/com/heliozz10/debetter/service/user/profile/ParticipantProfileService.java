package com.heliozz10.debetter.service.user.profile;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.City;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.profile.Profile;
import com.heliozz10.debetter.content.user.profile.institution.Institution;
import com.heliozz10.debetter.dto.user.profile.in.CityDto;
import com.heliozz10.debetter.dto.user.profile.in.InstitutionDto;
import com.heliozz10.debetter.repository.user.profile.CityRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.repository.user.profile.institution.InstitutionRepository;
import com.heliozz10.debetter.service.CommonService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Service
public class ParticipantProfileService implements ProfileService {
    private final EntityManager entityManager;

    private final ParticipantProfileRepository participantProfileRepository;

    private final CityRepository cityRepository;
    private final InstitutionRepository institutionRepository;

    private final CommonService commonService;

    @Override
    public ParticipantProfile getProfileById(Long id) {
        return participantProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participant profile not found"));
    }

    @Transactional(readOnly = true)
    public Page<City> getCities(String searchName, Pageable pageable) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<City> searchResult = searchSession.search(City.class)
                .where(f -> {
                    if (StringUtils.hasText(searchName)) {
                        return f.match()
                                .field("name")
                                .matching(searchName);
                    }
                    return f.matchAll();
                })
                .sort(f -> f.field("name"))
                .fetch((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(searchResult.hits(), pageable, searchResult.total().hitCount());
    }

    @Transactional(readOnly = true)
    public Page<Institution> getInstitutions(String searchName, Pageable pageable) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<Institution> searchResult = searchSession.search(Institution.class)
                .where(f -> {
                    if (StringUtils.hasText(searchName)) {
                        return f.match()
                                .field("name")
                                .matching(searchName);
                    }
                    return f.matchAll();
                })
                .sort(f -> f.field("name"))
                .fetch((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(searchResult.hits(), pageable, searchResult.total().hitCount());
    }

    @Transactional
    @Override
    public Profile createProfile(Long userId) {
        ParticipantProfile profile = new ParticipantProfile();

        User user = entityManager.getReference(User.class, userId);
        profile.setUser(user);

        ParticipantProfile savedProfile = participantProfileRepository.save(profile);
        user.setProfile(savedProfile);
        return savedProfile;
    }

    @Transactional
    public Profile createProfile(Long userId, CityDto cityDto, InstitutionDto institutionDto) {
        ParticipantProfile profile = new ParticipantProfile();

        User user = entityManager.getReference(User.class, userId);
        profile.setUser(user);

        if(cityDto != null && cityDto.name() != null) {
            City city = commonService.findOrCreateEntity(cityDto.name(), City.class, entityManager);
            profile.setCity(city);
        }

        if(institutionDto != null && institutionDto.name() != null) {
            Institution institution = commonService.findOrCreateEntity(institutionDto.name(), Institution.class, entityManager);
            profile.setInstitution(institution);
        }

        ParticipantProfile savedProfile = participantProfileRepository.save(profile);
        user.setProfile(savedProfile);
        return savedProfile;
    }
}