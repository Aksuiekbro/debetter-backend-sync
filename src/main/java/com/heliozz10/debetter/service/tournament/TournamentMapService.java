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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TournamentMapService {
    private final TournamentMapRepository tournamentMapRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentMapMapper tournamentMapMapper;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public TournamentMap getMap(Long tournamentId) {
        return tournamentMapRepository.findByTournamentId(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament map not found"));
    }

    @Transactional
    public TournamentMap createMap(TournamentMapFormDto dto, MultipartFile image, Long tournamentId) {
        if (image == null) {
            throw new IllegalArgumentException("Map image is required");
        }
        if (tournamentMapRepository.existsByTournamentId(tournamentId)) {
            throw new DataIntegrityViolationException("Tournament map already exists");
        }

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
        TournamentMap tournamentMap = tournamentMapMapper.toTournamentMap(dto);
        Url imageUrl = fileService.uploadImage(image, "tournament-maps", uniqueImageKey(tournamentId));
        tournamentMap.setTournament(tournament);
        tournamentMap.setImageUrl(imageUrl);
        tournament.setTournamentMap(tournamentMap);
        return tournamentMapRepository.save(tournamentMap);
    }

    @Transactional
    public TournamentMap updateMap(TournamentMapFormDto dto, MultipartFile image, Long tournamentId) {
        TournamentMap tournamentMap = getMap(tournamentId);
        tournamentMapMapper.updateTournamentMap(dto, tournamentMap);

        if (image != null) {
            Url previousImage = tournamentMap.getImageUrl();
            Url replacementImage = fileService.uploadImage(
                    image,
                    "tournament-maps",
                    uniqueImageKey(tournamentId)
            );
            fileService.deletePhysicalFileAfterCommit(previousImage);
            tournamentMap.setImageUrl(replacementImage);
        }

        return tournamentMapRepository.save(tournamentMap);
    }

    private String uniqueImageKey(Long tournamentId) {
        return tournamentId + "-" + UUID.randomUUID();
    }
}
