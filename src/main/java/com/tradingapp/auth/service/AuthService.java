package com.tradingapp.auth.service;

import com.tradingapp.auth.dto.AuthResponse;
import com.tradingapp.auth.dto.LoginRequest;
import com.tradingapp.auth.dto.RegisterRequest;
import com.tradingapp.auth.dto.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String rawRefreshToken);
    void logout(Long userId);
}
