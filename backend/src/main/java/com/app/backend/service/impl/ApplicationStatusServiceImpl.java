package com.app.backend.service.impl;

import com.app.backend.service.ApplicationStatusService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ApplicationStatusServiceImpl implements ApplicationStatusService {

    @Override
    public Map<String, String> getStatus() {
        return Map.of("status", "UP");
    }
}
