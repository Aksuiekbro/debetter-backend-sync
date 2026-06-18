package com.heliozz10.debetter.service.util.media;

import com.heliozz10.debetter.repository.util.media.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    private UrlRepository urlRepository;

    @Mock
    private FileUploadProperties fileUploadProperties;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(urlRepository, fileUploadProperties);
    }

    @Test
    void deleteFileIgnoresMissingUrl() {
        fileService.deleteFile(null);

        verifyNoInteractions(urlRepository, fileUploadProperties);
    }

    @Test
    void deleteFilesIgnoresMissingAndNullUrls() {
        fileService.deleteFiles(null);
        fileService.deleteFiles(Arrays.asList(null, null));
        fileService.deleteFiles(List.of());

        verifyNoInteractions(urlRepository, fileUploadProperties);
    }
}
