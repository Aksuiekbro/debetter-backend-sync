package com.heliozz10.debetter.service.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class FileService {
    private final UrlRepository urlRepository;
    private final FileUploadProperties fileUploadProperties;

    @Transactional
    public Url uploadImage(MultipartFile file, String path, String fileName) {
        validateImage(file);
        return saveFile(file, "images/" + path, fileName, true);
    }

    @Transactional
    public List<Url> uploadImages(Map<String, MultipartFile> files, String path) {
        for (MultipartFile file : files.values()) {
            validateImage(file);
        }
        return saveFiles(files, "images/" + path, true);
    }

    @Transactional
    public Url uploadFile(MultipartFile file, String path, String fileName) {
        return saveFile(file, path, fileName, false);
    }

    @Transactional
    public List<Url> uploadFiles(Map<String, MultipartFile> files, String path) {
        return saveFiles(files, path, false);
    }

    @Transactional
    public void deleteFile(Url url) {
        if (url == null) {
            return;
        }

        Path storagePath = Paths.get(getStoragePathFromUrl(url));
        deleteStoredFile(storagePath);
        urlRepository.delete(url);
    }

    public void deletePhysicalFileAfterCommit(Url url) {
        if (url == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (urlRepository.countByUrl(url.getUrl()) <= 1) {
                deleteStoredFile(Paths.get(getStoragePathFromUrl(url)));
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (urlRepository.countByUrl(url.getUrl()) == 0) {
                        deleteStoredFile(Paths.get(getStoragePathFromUrl(url)));
                    }
                } catch (RuntimeException exception) {
                    log.error("Database changes committed, but stored file cleanup failed: {}", url.getUrl(), exception);
                }
            }
        });
    }

    private void deleteStoredFile(Path storagePath) {
        try {
            Files.deleteIfExists(storagePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete stored file", e);
        }
    }

    @Transactional
    public void deleteFiles(Collection<Url> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        for (Url url : urls) {
            deleteFile(url);
        }
    }

    public void deletePhysicalFilesAfterCommit(Collection<Url> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        for (Url url : urls) {
            deletePhysicalFileAfterCommit(url);
        }
    }

    public Path resolveFilePathByUrl(String url) {
        Url entity = urlRepository.findFirstByUrlOrderByIdAsc(url)
                .orElseThrow(() -> new IllegalArgumentException("File not found for URL: " + url));
        return Paths.get(getStoragePathFromUrl(entity));
    }

    //PRIVATE HELPERS

    private Url saveFile(MultipartFile file, String path, String fileName, boolean cleanUpOnRollback) {
        Path storedFile = uploadFileRaw(file, path, fileName);
        if (cleanUpOnRollback) {
            deleteStoredFileAfterRollback(storedFile);
        }

        Url url = new Url();
        url.setUrl(buildPublicUrl(path, fileName, file.getOriginalFilename()));

        return urlRepository.save(url);
    }

    private List<Url> saveFiles(Map<String, MultipartFile> files, String path, boolean cleanUpOnRollback) {
        List<Url> urls = new ArrayList<>();

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            MultipartFile file = entry.getValue();
            String fileName = entry.getKey();

            Path storedFile = uploadFileRaw(file, path, fileName);
            if (cleanUpOnRollback) {
                deleteStoredFileAfterRollback(storedFile);
            }

            Url url = new Url();
            url.setUrl(buildPublicUrl(path, fileName, file.getOriginalFilename()));
            urls.add(url);
        }

        return urlRepository.saveAll(urls);
    }

    private Path uploadFileRaw(MultipartFile file, String path, String fileName) {
        Path destination = null;
        boolean copyStarted = false;
        try {
            Path uploadDir = Paths.get(fileUploadProperties.getStoragePath(), path);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            String uniqueFileName = fileName + "." + fileExtension;

            destination = uploadDir.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                copyStarted = true;
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } catch (IOException e) {
            if (copyStarted && destination != null) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }
            throw new FileStorageException("Failed to save uploaded file", e);
        }
    }

    private void deleteStoredFileAfterRollback(Path storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        deleteStoredFile(storagePath);
                    } catch (RuntimeException exception) {
                        log.error("Transaction rolled back, but uploaded file could not be deleted: {}", storagePath, exception);
                    }
                }
            }
        });
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' is empty");
        }

        if (file.getSize() > fileUploadProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename()
                    + "' exceeds maximum allowed size of "
                    + fileUploadProperties.getMaxFileSize() / 1024 / 1024 + " MB");
        }

        String fileExtension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!List.of("jpg", "jpeg", "png").contains(fileExtension)) {
            throw new IllegalArgumentException("Invalid file extension for file '" + file.getOriginalFilename() + "'");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid MIME type for file '" + file.getOriginalFilename() + "'");
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1)
                : "";
    }

    private String getStoragePathFromUrl(Url url) {
        String urlPath = url.getUrl();
        String publicUrlPrefix = fileUploadProperties.getPublicUrlPrefix();
        String relativePath = urlPath.substring(publicUrlPrefix.length());
        return Paths.get(fileUploadProperties.getStoragePath(), relativePath).toString();
    }

    private String buildPublicUrl(String path, String fileName, String originalFileName) {
        String extension = getFileExtension(originalFileName).toLowerCase();
        return fileUploadProperties.getPublicUrlPrefix() + path + "/" + fileName + "." + extension;
    }
}
