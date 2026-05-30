package com.app.backend.service;

import com.app.backend.entity.AuditLog;

public interface AuditService {

    void recordMutation(AuditLog entry);
}
