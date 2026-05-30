package com.app.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Local filesystem storage for uploaded property images. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Absolute or relative directory where uploaded bytes are stored. */
    private String root = "./data/uploads";

    /** Maximum upload size per file (bytes). Default 5 MiB. */
    private long maxImageBytes = 5L * 1024 * 1024;

    /** Allowed image MIME types (comma-separated in YAML optional). */
    private String allowedContentTypes = "image/jpeg,image/png,image/gif,image/webp";
}
