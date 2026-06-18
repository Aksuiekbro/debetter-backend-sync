package com.heliozz10.debetter.service.user;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.dto.user.in.UserRegistrationDto;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.CityRepository;
import com.heliozz10.debetter.repository.user.profile.institution.InstitutionRepository;
import com.heliozz10.debetter.service.CommonService;
import com.heliozz10.debetter.service.user.profile.OrganizerProfileService;
import com.heliozz10.debetter.service.user.profile.ParticipantProfileService;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ParticipantProfileService participantProfileService;

    @Mock
    private OrganizerProfileService organizerProfileService;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private CommonService commonService;

    @Mock
    private FileService fileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                entityManager,
                userRepository,
                userMapper,
                participantProfileService,
                organizerProfileService,
                cityRepository,
                institutionRepository,
                commonService,
                fileService,
                passwordEncoder
        );
    }

    @Test
    void createUserRejectsDuplicateUsernameOrEmailBeforeSaving() {
        UserRegistrationDto dto = new UserRegistrationDto(
                "dupeuser",
                "Test12345!",
                "dupe@example.com",
                "Dupe",
                "User",
                Role.ORGANIZER,
                null,
                null
        );
        when(userRepository.existsByUsernameOrEmail(dto.username(), dto.email())).thenReturn(true);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> userService.createUser(dto)
        );

        assertEquals("Username or email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper, passwordEncoder, organizerProfileService, participantProfileService);
    }
}
