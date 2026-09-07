package com.heliozz10.debetter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultipartConfigurationTest {
    @Test
    void dockerConfigurationAcceptsTheDocumentedNewsUploadSizes() throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        for(PropertySource<?> source : new YamlPropertySourceLoader().load(
                "application-docker",
                new ClassPathResource("application-docker.yml")
        )) {
            sources.addLast(source);
        }
        PropertySourcesPropertyResolver properties = new PropertySourcesPropertyResolver(sources);

        assertEquals("5MB", properties.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("60MB", properties.getProperty("spring.servlet.multipart.max-request-size"));
    }
}
