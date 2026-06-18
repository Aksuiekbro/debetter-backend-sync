package com.heliozz10.debetter.controller.util.media;

import com.heliozz10.debetter.service.util.media.FileService;
import com.heliozz10.debetter.service.util.media.FileUploadProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/uploads")
public class FileController {
    private final FileService fileService;

    private final Environment environment;

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        String requestUri = request.getRequestURI(); // e.g. /api/uploads/images/foo/bar.png
        final String servletPath = environment.getProperty("spring.mvc.servlet.path");
        String fullUrl = requestUri.startsWith(servletPath) ? requestUri.substring(servletPath.length()) : requestUri;

        Path filePath = fileService.resolveFilePathByUrl(fullUrl);

        Resource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
