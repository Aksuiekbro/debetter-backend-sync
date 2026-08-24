package com.heliozz10.debetter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaUploadConfigurationTest {
    @Test
    void dockerMultipartLimitMatchesTheApplicationImageLimit() throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source : new YamlPropertySourceLoader().load(
                "application-docker",
                new ClassPathResource("application-docker.yml")
        )) {
            sources.addLast(source);
        }
        PropertySourcesPropertyResolver properties = new PropertySourcesPropertyResolver(sources);

        assertEquals("5MB", properties.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("60MB", properties.getProperty("spring.servlet.multipart.max-request-size"));
        assertEquals(
                5L * 1024 * 1024,
                properties.getProperty("app.file-upload.max-file-size", Long.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void dockerComposePersistsUploadedMediaOutsideTheApplicationContainer() throws IOException {
        Map<String, Object> compose;
        try (InputStream input = Files.newInputStream(Path.of("docker-compose.yml"))) {
            compose = new Yaml().load(input);
        }

        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        Map<String, Object> app = (Map<String, Object>) services.get("app");
        List<String> appVolumes = (List<String>) app.get("volumes");
        Map<String, Object> declaredVolumes = (Map<String, Object>) compose.get("volumes");

        assertTrue(appVolumes.contains("uploads-data:/var/www/debetter/uploads"));
        assertTrue(declaredVolumes.containsKey("uploads-data"));
    }
}
