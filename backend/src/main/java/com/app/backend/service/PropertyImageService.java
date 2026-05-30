package com.app.backend.service;

import com.app.backend.dto.media.PropertyImageResponse;
import com.app.backend.entity.LinkedEntityType;
import com.app.backend.security.UserPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PropertyImageService {

    record ImageFilePayload(Resource resource, String contentType) {}

    PropertyImageResponse upload(UserPrincipal actor, LinkedEntityType entityType, Long entityId, MultipartFile file);

    List<PropertyImageResponse> list(UserPrincipal viewer, LinkedEntityType entityType, Long entityId);

    ImageFilePayload loadFile(UserPrincipal viewer, Long imageId);

    void delete(UserPrincipal actor, Long imageId);

    /** Removes DB rows and files when parent plot/rental is deleted (internal). */
    void deleteAllForEntity(LinkedEntityType entityType, Long entityId);
}
