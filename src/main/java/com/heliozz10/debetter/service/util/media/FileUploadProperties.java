package com.heliozz10.debetter.service.util.media;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.file-upload")
@Data
public class FileUploadProperties {
    private String storagePath;
    private String publicUrlPrefix;
    private long maxFileSize;
}
