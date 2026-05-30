package com.app.backend.support;

import com.app.backend.entity.BaseEntity;

import java.time.Instant;

public final class SoftDelete {

    private SoftDelete() {
    }

    public static void mark(BaseEntity entity) {
        entity.setDeletedAt(Instant.now());
    }
}
