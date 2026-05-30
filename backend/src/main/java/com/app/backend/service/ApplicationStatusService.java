package com.app.backend.service;

import java.util.Map;

/**
 * Minimal service contract for wiring checks; extend with domain services later.
 */
public interface ApplicationStatusService {

    Map<String, String> getStatus();
}
