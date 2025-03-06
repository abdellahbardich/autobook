package com.autobook.usermanagement.service;

import com.autobook.usermanagement.dto.AuthRequest;
import com.autobook.usermanagement.dto.AuthResponse;
import com.autobook.usermanagement.dto.UserRegistrationDto;

public interface AuthService {
    AuthResponse login(AuthRequest authRequest);
    AuthResponse register(UserRegistrationDto registrationDto);
}