package com.heliozz10.debetter.service.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @TempDir
    Path uploadDirectory;

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

    @Test
    void oldPhysicalFileIsRemovedOnlyAfterTheDatabaseTransactionCommits() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("images/announcements"))
                .resolve("53-old.jpg");
        Files.writeString(storedFile, "old photo");
        Url url = imageUrl("/uploads/images/announcements/53-old.jpg");
        AtomicBoolean transactionCommitted = new AtomicBoolean(false);
        when(urlRepository.countByUrl(url.getUrl()))
                .thenAnswer(invocation -> transactionCommitted.get() ? 0L : 1L);
        configureStorage();

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFileAfterCommit(url);

            assertTrue(Files.exists(storedFile));
            verifyNoInteractions(urlRepository);
            transactionCommitted.set(true);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertFalse(Files.exists(storedFile));
            verify(urlRepository).countByUrl(url.getUrl());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void replacingOneLegacyAnnouncementPreservesAFileStillReferencedByAnotherAnnouncement() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("announcements"))
                .resolve("53.jpg");
        Files.writeString(storedFile, "legacy shared photo");
        Url url = imageUrl("/uploads/announcements/53.jpg");
        when(urlRepository.countByUrl(url.getUrl())).thenReturn(1L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFileAfterCommit(url);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertTrue(Files.exists(storedFile));
            verify(urlRepository).countByUrl(url.getUrl());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadedImageIsRemovedWhenTheDatabaseTransactionRollsBack() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "replacement.png", "image/png", "new photo".getBytes()
        );
        configureStorage();
        when(fileUploadProperties.getMaxFileSize()).thenReturn(5L * 1024 * 1024);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.uploadImage(image, "announcements", "53-new");
            Path storedFile = uploadDirectory.resolve("images/announcements/53-new.png");
            assertTrue(Files.exists(storedFile));

            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
            );

            assertFalse(Files.exists(storedFile));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void interruptedUploadRemovesThePartialFileAndReturnsAUsefulStorageError() throws IOException {
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getSize()).thenReturn(4L);
        when(image.getOriginalFilename()).thenReturn("broken.jpg");
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getInputStream()).thenReturn(new InputStream() {
            private int bytesRead;

            @Override
            public int read() throws IOException {
                if (bytesRead == 2) {
                    throw new IOException("Simulated interrupted upload");
                }
                bytesRead++;
                return 'x';
            }
        });
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getMaxFileSize()).thenReturn(5L * 1024 * 1024);

        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> fileService.uploadImage(image, "tournament-maps", "broken-id")
        );

        assertEquals("Failed to save uploaded file", exception.getMessage());
        assertFalse(Files.exists(uploadDirectory.resolve("images/tournament-maps/broken-id.jpg")));
        verifyNoInteractions(urlRepository);
    }

    private void configureStorage() {
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");
    }

    private static Url imageUrl(String value) {
        Url url = new Url();
        url.setUrl(value);
        return url;
    }
}
