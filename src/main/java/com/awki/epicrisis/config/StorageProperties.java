package com.awki.epicrisis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "epicrisis.storage")
@Getter
@Setter
public class StorageProperties {
    private String mode = "local";
    private String localPath = "uploads/epicrisis";
}
