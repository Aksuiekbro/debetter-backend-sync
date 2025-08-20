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
    private final Environment environment;

    private final UrlRepository urlRepository;

    private final FileUploadProperties fileUploadProperties;

    //TODO: dont forget about security for files. Files should be retrieved after resolving the url entity. flow:
    // url (/uploads/*) -> security -> url entity -> file
    @Transactional
    public Url uploadImage(MultipartFile file, String path, String fileName) {
        validateImage(file);

        return uploadFile(file, path, fileName);
    }

    @Transactional
    public List<Url> uploadImages(Map<String, MultipartFile> files, String path) {
        for(Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            validateImage(entry.getValue());
        }

        return uploadFiles(files, path);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' is empty");
        }

        if (file.getSize() > fileUploadProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' exceeds maximum allowed size of " +
                    fileUploadProperties.getMaxFileSize() / 1024 / 1024 + " MB");
        }

        String fileExtension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if(!List.of("jpg", "jpeg", "png").contains(fileExtension)) {
            throw new IllegalArgumentException("Invalid file extension for file '" + file.getOriginalFilename() + "': " + fileExtension);
        }

        try {
            String mimeType = Files.probeContentType(Paths.get(file.getOriginalFilename()));
            if (mimeType == null || !mimeType.startsWith("image/")) {
                throw new IllegalArgumentException("Invalid MIME type for file '" + file.getOriginalFilename() + "': " + mimeType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to determine file type for file '" + file.getOriginalFilename() + "'", e);
        }
    }

    /**
     * @param file
     * @param path must not have slashes at the beginning or end
     * @param fileName must be unique and without extension
     * @return
     */
    public Url uploadFile(MultipartFile file, String path, String fileName) {
        uploadFileRaw(file, path, fileName);
        Url url = new Url();
        url.setUrl(fileUploadProperties.getPublicUrlPrefix() + path + "/" + fileName);
        return urlRepository.save(url);
    }

    /**
     * uploads multiple files
     * @param files map of file name to file
     * @param path path to store files
     * @return list of urls
     */
    @Transactional
    public List<Url> uploadFiles(Map<String, MultipartFile> files, String path) {
        List<Url> urls = new ArrayList<>();
        for(Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            uploadFileRaw(entry.getValue(), path, entry.getKey());
            Url url = new Url();
            url.setUrl(fileUploadProperties.getPublicUrlPrefix() + path + "/" + entry.getKey());
            urls.add(url);
        }
        urlRepository.saveAll(urls);
        return urls;
    }

    /**
     * make sure to use this method with file service generated urls
     * @param url
     */
    public void deleteFile(Url url) {
        String storagePath = getStoragePathFromUrl(url);
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public void deleteFiles(Collection<Url> urls) {
        for (Url url : urls) {
            deleteFile(url);
        }
    }

    private void uploadFileRaw(MultipartFile file, String path, String fileName) {
        try {
            Path uploadDir = Paths.get(fileUploadProperties.getStoragePath(), path);
            if(!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            String uniqueFileName = fileName + "." + fileExtension;

            Path destination = uploadDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
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
}
