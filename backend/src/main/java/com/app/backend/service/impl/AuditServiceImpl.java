package com.app.backend.service.impl;

import com.app.backend.entity.AuditLog;
import com.app.backend.repository.AuditLogRepository;
import com.app.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void recordMutation(AuditLog entry) {
        auditLogRepository.save(entry);
    }
}
