package com.autobook.userservice.controller;

import com.autobook.userservice.dto.UserDto;
import com.autobook.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    public ResponseEntity<UserDto.UserResponse> registerUser(@Valid @RequestBody UserDto.RegisterRequest registerRequest) {
        UserDto.UserResponse userResponse = userService.registerUser(registerRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserDto.JwtResponse> authenticateUser(@Valid @RequestBody UserDto.LoginRequest loginRequest) {
        UserDto.JwtResponse jwtResponse = userService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<UserDto.UserResponse> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto.UserResponse userResponse = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(userResponse);
    }
}