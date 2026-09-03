package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.LoginRequest;
import com.timecapsule.wishes.dto.request.RefreshTokenRequest;
import com.timecapsule.wishes.dto.request.RegisterRequest;
import com.timecapsule.wishes.dto.response.AuthResponse;
import com.timecapsule.wishes.dto.response.UserResponse;
import com.timecapsule.wishes.security.UserPrincipal;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserResponse getCurrentUser(UserPrincipal principal);
}
