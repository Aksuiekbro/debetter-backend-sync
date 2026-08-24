package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentMap;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tournament.in.TournamentMapFormDto;
import com.heliozz10.debetter.mapper.tournament.TournamentMapMapper;
import com.heliozz10.debetter.repository.tournament.TournamentMapRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentMapServiceTest {
    @Mock
    private TournamentMapRepository tournamentMapRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentMapMapper tournamentMapMapper;
    @Mock
    private FileService fileService;

    private TournamentMapService tournamentMapService;

    @BeforeEach
    void setUp() {
        tournamentMapService = new TournamentMapService(
                tournamentMapRepository,
                tournamentRepository,
                tournamentMapMapper,
                fileService
        );
    }

    @Test
    void createsMapWithTournamentMetadataAndImage() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Venue map", "Rooms and entrances");
        MockMultipartFile image = image("venue.png");
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        TournamentMap tournamentMap = new TournamentMap();
        Url uploadedImage = imageUrl("/uploads/images/tournament-maps/53-unique.png");

        when(tournamentMapRepository.existsByTournamentId(53L)).thenReturn(false);
        when(tournamentRepository.findById(53L)).thenReturn(Optional.of(tournament));
        when(tournamentMapMapper.toTournamentMap(dto)).thenReturn(tournamentMap);
        when(fileService.uploadImage(eq(image), eq("tournament-maps"), anyString()))
                .thenReturn(uploadedImage);
        when(tournamentMapRepository.save(tournamentMap)).thenReturn(tournamentMap);

        TournamentMap saved = tournamentMapService.createMap(dto, image, 53L);

        assertSame(tournamentMap, saved);
        assertSame(tournament, saved.getTournament());
        assertSame(uploadedImage, saved.getImageUrl());
    }

    @Test
    void createRequiresAnImage() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Venue map", "Rooms and entrances");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tournamentMapService.createMap(dto, null, 53L)
        );

        org.junit.jupiter.api.Assertions.assertEquals("Map image is required", exception.getMessage());
        verify(tournamentMapRepository, never()).save(any());
    }

    @Test
    void createRejectsASecondMapForTheTournament() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Venue map", "Rooms and entrances");
        MockMultipartFile image = image("venue.png");
        when(tournamentMapRepository.existsByTournamentId(53L)).thenReturn(true);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> tournamentMapService.createMap(dto, image, 53L)
        );

        org.junit.jupiter.api.Assertions.assertEquals("Tournament map already exists", exception.getMessage());
        verify(fileService, never()).uploadImage(any(), anyString(), anyString());
    }

    @Test
    void metadataOnlyUpdateKeepsTheExistingImage() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Updated map", "Updated directions");
        Url existingImage = imageUrl("/uploads/images/tournament-maps/53-existing.png");
        TournamentMap tournamentMap = new TournamentMap();
        tournamentMap.setImageUrl(existingImage);
        when(tournamentMapRepository.findByTournamentId(53L)).thenReturn(Optional.of(tournamentMap));
        when(tournamentMapRepository.save(tournamentMap)).thenReturn(tournamentMap);

        TournamentMap updated = tournamentMapService.updateMap(dto, null, 53L);

        assertSame(tournamentMap, updated);
        assertSame(existingImage, updated.getImageUrl());
        verify(tournamentMapMapper).updateTournamentMap(dto, tournamentMap);
        verify(fileService, never()).uploadImage(any(), anyString(), anyString());
        verify(fileService, never()).deletePhysicalFileAfterCommit(any());
    }

    @Test
    void imageReplacementUploadsFirstThenRetiresOnlyThePreviousImage() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Updated map", "Updated directions");
        MockMultipartFile replacement = image("replacement.png");
        Url existingImage = imageUrl("/uploads/images/tournament-maps/53-existing.png");
        Url replacementImage = imageUrl("/uploads/images/tournament-maps/53-new.png");
        TournamentMap tournamentMap = new TournamentMap();
        tournamentMap.setImageUrl(existingImage);

        when(tournamentMapRepository.findByTournamentId(53L)).thenReturn(Optional.of(tournamentMap));
        when(fileService.uploadImage(eq(replacement), eq("tournament-maps"), anyString()))
                .thenReturn(replacementImage);
        when(tournamentMapRepository.save(tournamentMap)).thenReturn(tournamentMap);

        TournamentMap updated = tournamentMapService.updateMap(dto, replacement, 53L);

        assertSame(replacementImage, updated.getImageUrl());
        InOrder lifecycle = inOrder(fileService);
        lifecycle.verify(fileService).uploadImage(eq(replacement), eq("tournament-maps"), anyString());
        lifecycle.verify(fileService).deletePhysicalFileAfterCommit(existingImage);
    }

    @Test
    void uploadFailureLeavesTheExistingMapImageUnchanged() {
        TournamentMapFormDto dto = new TournamentMapFormDto("Updated map", "Updated directions");
        MockMultipartFile replacement = image("replacement.png");
        Url existingImage = imageUrl("/uploads/images/tournament-maps/53-existing.png");
        TournamentMap tournamentMap = new TournamentMap();
        tournamentMap.setImageUrl(existingImage);
        when(tournamentMapRepository.findByTournamentId(53L)).thenReturn(Optional.of(tournamentMap));
        when(fileService.uploadImage(eq(replacement), eq("tournament-maps"), anyString()))
                .thenThrow(new RuntimeException("Failed to save file"));

        assertThrows(RuntimeException.class, () -> tournamentMapService.updateMap(dto, replacement, 53L));

        assertSame(existingImage, tournamentMap.getImageUrl());
        verify(fileService, never()).deletePhysicalFileAfterCommit(existingImage);
        verify(tournamentMapRepository, never()).save(any());
    }

    @Test
    void missingMapReturnsNotFound() {
        when(tournamentMapRepository.findByTournamentId(53L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> tournamentMapService.getMap(53L)
        );

        org.junit.jupiter.api.Assertions.assertEquals("Tournament map not found", exception.getMessage());
    }

    private static MockMultipartFile image(String filename) {
        return new MockMultipartFile("image", filename, "image/png", "image bytes".getBytes());
    }

    private static Url imageUrl(String value) {
        Url url = new Url();
        url.setUrl(value);
        return url;
    }
}
