package com.app.backend.service;

import com.app.backend.dto.auth.AuthResponse;
import com.app.backend.dto.auth.LoginRequest;
import com.app.backend.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);
}
