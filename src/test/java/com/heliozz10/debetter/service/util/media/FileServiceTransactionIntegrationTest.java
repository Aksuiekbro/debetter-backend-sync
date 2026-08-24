package com.heliozz10.debetter.service.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:file_service_transaction_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/file-service-transaction",
        "app.file-upload.storage-path=target/test-uploads/file-service-transaction"
})
class FileServiceTransactionIntegrationTest {
    private static final Path STORAGE_ROOT = Path.of("target/test-uploads/file-service-transaction");

    @Autowired
    private FileService fileService;
    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Path storedFile;

    @AfterEach
    void cleanUp() throws IOException {
        urlRepository.deleteAll();
        if (storedFile != null) {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void committedDeletionOfEveryLegacyReferenceRemovesTheSharedPhysicalFile() throws IOException {
        String filename = UUID.randomUUID() + ".jpg";
        String publicUrl = "/uploads/images/announcements/" + filename;
        storedFile = storedFile(filename);
        List<Url> references = urlRepository.saveAllAndFlush(List.of(url(publicUrl), url(publicUrl)));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            references.forEach(fileService::deletePhysicalFileAfterCommit);
            urlRepository.deleteAll(references);
        });

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void committedDeletionPreservesAFileWithOneRemainingLegacyReference() throws IOException {
        String filename = UUID.randomUUID() + ".jpg";
        String publicUrl = "/uploads/images/announcements/" + filename;
        storedFile = storedFile(filename);
        List<Url> references = urlRepository.saveAllAndFlush(List.of(url(publicUrl), url(publicUrl)));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            fileService.deletePhysicalFileAfterCommit(references.getFirst());
            urlRepository.delete(references.getFirst());
        });

        assertTrue(Files.exists(storedFile));
    }

    private Path storedFile(String filename) throws IOException {
        Path directory = Files.createDirectories(STORAGE_ROOT.resolve("images/announcements"));
        Path file = directory.resolve(filename);
        Files.writeString(file, "legacy shared photo");
        return file;
    }

    private static Url url(String value) {
        Url url = new Url();
        url.setUrl(value);
        return url;
    }
}
