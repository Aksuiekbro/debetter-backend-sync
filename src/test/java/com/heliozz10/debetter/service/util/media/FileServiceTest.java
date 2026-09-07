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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void deletePhysicalFileAfterCommitWaitsBeforeRemovingStoredFileWithoutDeletingItsUrlRow() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("images/news"))
                .resolve("photo.jpg");
        Files.writeString(storedFile, "photo contents");
        Url url = new Url();
        url.setUrl("/uploads/images/news/photo.jpg");

        AtomicLong remainingReferences = new AtomicLong(1L);
        when(urlRepository.countByUrl("/uploads/images/news/photo.jpg"))
                .thenAnswer(invocation -> remainingReferences.get());
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFileAfterCommit(url);

            assertTrue(Files.exists(storedFile));
            verifyNoInteractions(urlRepository);

            remainingReferences.set(0L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertFalse(Files.exists(storedFile));
            verify(urlRepository).countByUrl(url.getUrl());
            verify(urlRepository, never()).delete(any(Url.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletePhysicalFileAfterCommitPreservesALegacyPathReferencedByAnotherUrlRow() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("announcements"))
                .resolve("53.jpg");
        Files.writeString(storedFile, "shared legacy photo contents");
        Url url = new Url();
        url.setId(101L);
        url.setUrl("/uploads/announcements/53.jpg");

        AtomicLong remainingReferences = new AtomicLong(2L);
        when(urlRepository.countByUrl("/uploads/announcements/53.jpg"))
                .thenAnswer(invocation -> remainingReferences.get());

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFileAfterCommit(url);
            verifyNoInteractions(urlRepository);
            remainingReferences.set(1L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertTrue(Files.exists(storedFile));
            verify(urlRepository).countByUrl("/uploads/announcements/53.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletePhysicalFilesAfterCommitRemovesFileWhenAllDuplicateReferencesAreDeleted() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("announcements"))
                .resolve("53.jpg");
        Files.writeString(storedFile, "shared legacy photo contents");
        Url firstUrl = new Url();
        firstUrl.setId(101L);
        firstUrl.setUrl("/uploads/announcements/53.jpg");
        Url secondUrl = new Url();
        secondUrl.setId(102L);
        secondUrl.setUrl(firstUrl.getUrl());

        AtomicLong remainingReferences = new AtomicLong(2L);
        when(urlRepository.countByUrl(firstUrl.getUrl())).thenAnswer(invocation -> remainingReferences.get());
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFilesAfterCommit(List.of(firstUrl, secondUrl));
            assertTrue(Files.exists(storedFile));
            verifyNoInteractions(urlRepository);

            remainingReferences.set(0L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertFalse(Files.exists(storedFile));
            verify(urlRepository, times(2)).countByUrl(firstUrl.getUrl());
            verify(urlRepository, never()).delete(any(Url.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletePhysicalFileAfterCommitPreservesStoredFileWhenTransactionRollsBack() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("images/news"))
                .resolve("photo.jpg");
        Files.writeString(storedFile, "photo contents");
        Url url = new Url();
        url.setUrl("/uploads/images/news/photo.jpg");

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePhysicalFileAfterCommit(url);
            assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
            );

            assertTrue(Files.exists(storedFile));
            verifyNoInteractions(urlRepository, fileUploadProperties);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void regularDeleteFileRemainsImmediateForSamePathReplacementCallers() throws IOException {
        Path storedFile = Files.createDirectories(uploadDirectory.resolve("images/announcements"))
                .resolve("42.jpg");
        Files.writeString(storedFile, "old photo contents");
        Url url = new Url();
        url.setUrl("/uploads/images/announcements/42.jpg");

        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deleteFile(url);

            assertFalse(Files.exists(storedFile));
            verify(urlRepository).delete(url);
            assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadImageNormalizesUppercaseExtensionInStoredPathAndPublicUrl() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "PHOTO.JPG",
                "image/jpeg",
                "photo contents".getBytes()
        );
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");
        when(fileUploadProperties.getMaxFileSize()).thenReturn(5L * 1024 * 1024);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Url uploaded = fileService.uploadImage(image, "news/images", "photo-id");

        assertEquals("/uploads/images/news/images/photo-id.jpg", uploaded.getUrl());
        assertTrue(Files.exists(uploadDirectory.resolve("images/news/images/photo-id.jpg")));
    }

    @Test
    void uploadImageRemovesStoredFileWhenTransactionRollsBack() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "rollback.png",
                "image/png",
                "photo contents".getBytes()
        );
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getPublicUrlPrefix()).thenReturn("/uploads/");
        when(fileUploadProperties.getMaxFileSize()).thenReturn(5L * 1024 * 1024);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.uploadImage(image, "news/images", "rollback-id");
            Path storedFile = uploadDirectory.resolve("images/news/images/rollback-id.png");
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
    void uploadImageRemovesPartialDestinationWhenTheInputStreamFails() throws IOException {
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getSize()).thenReturn(4L);
        when(image.getOriginalFilename()).thenReturn("broken.jpg");
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getInputStream()).thenReturn(new InputStream() {
            private int bytesRead;

            @Override
            public int read() throws IOException {
                if(bytesRead == 2) {
                    throw new IOException("Simulated interrupted upload");
                }
                bytesRead++;
                return 'x';
            }
        });
        when(fileUploadProperties.getStoragePath()).thenReturn(uploadDirectory.toString());
        when(fileUploadProperties.getMaxFileSize()).thenReturn(5L * 1024 * 1024);

        assertThrows(
                RuntimeException.class,
                () -> fileService.uploadImage(image, "news/images", "broken-id")
        );

        assertFalse(Files.exists(uploadDirectory.resolve("images/news/images/broken-id.jpg")));
        verifyNoInteractions(urlRepository);
    }
}
