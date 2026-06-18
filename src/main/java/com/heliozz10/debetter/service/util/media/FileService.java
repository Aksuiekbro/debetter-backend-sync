package com.heliozz10.debetter.service.util.media;

import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RequiredArgsConstructor
@Service
public class FileService {
    private final UrlRepository urlRepository;
    private final FileUploadProperties fileUploadProperties;

    @Transactional
    public Url uploadImage(MultipartFile file, String path, String fileName) {
        validateImage(file);
        return saveFile(file, "images/" + path, fileName);
    }

    @Transactional
    public List<Url> uploadImages(Map<String, MultipartFile> files, String path) {
        for (MultipartFile file : files.values()) {
            validateImage(file);
        }
        return saveFiles(files, "images/" + path);
    }

    @Transactional
    public Url uploadFile(MultipartFile file, String path, String fileName) {
        return saveFile(file, path, fileName);
    }

    @Transactional
    public List<Url> uploadFiles(Map<String, MultipartFile> files, String path) {
        return saveFiles(files, path);
    }

    @Transactional
    public void deleteFile(Url url) {
        if (url == null) {
            return;
        }

        String storagePath = getStoragePathFromUrl(url);
        try {
            Files.deleteIfExists(Paths.get(storagePath));
            urlRepository.delete(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
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

    public Path resolveFilePathByUrl(String url) {
        Url entity = urlRepository.findByUrl(url)
                .orElseThrow(() -> new IllegalArgumentException("File not found for URL: " + url));
        return Paths.get(getStoragePathFromUrl(entity));
    }

    //PRIVATE HELPERS

    private Url saveFile(MultipartFile file, String path, String fileName) {
        uploadFileRaw(file, path, fileName);

        Url url = new Url();
        url.setUrl(buildPublicUrl(path, fileName, file.getOriginalFilename()));

        return urlRepository.save(url);
    }

    private List<Url> saveFiles(Map<String, MultipartFile> files, String path) {
        List<Url> urls = new ArrayList<>();

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            MultipartFile file = entry.getValue();
            String fileName = entry.getKey();

            uploadFileRaw(file, path, fileName);

            Url url = new Url();
            url.setUrl(buildPublicUrl(path, fileName, file.getOriginalFilename()));
            urls.add(url);
        }

        return urlRepository.saveAll(urls);
    }

    private void uploadFileRaw(MultipartFile file, String path, String fileName) {
        try {
            Path uploadDir = Paths.get(fileUploadProperties.getStoragePath(), path);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            String uniqueFileName = fileName + "." + fileExtension;

            Path destination = uploadDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
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
        String extension = getFileExtension(originalFileName);
        return fileUploadProperties.getPublicUrlPrefix() + path + "/" + fileName + "." + extension;
    }
}
