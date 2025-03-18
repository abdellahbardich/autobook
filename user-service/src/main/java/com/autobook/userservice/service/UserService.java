package com.autobook.userservice.service;

import com.autobook.userservice.dto.UserDto;
import com.autobook.userservice.entity.User;
import com.autobook.userservice.repository.UserRepository;
import com.autobook.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserDto.UserResponse registerUser(UserDto.RegisterRequest registerRequest) {
        // Check if username or email exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with username: {}", savedUser.getUsername());

        return mapUserToResponse(savedUser);
    }

    public UserDto.JwtResponse authenticateUser(UserDto.LoginRequest loginRequest) {
        String usernameOrEmail = loginRequest.getUsernameOrEmail();
        String username;
        User user;

        // Determine if input is email or username
        if (usernameOrEmail.contains("@")) {
            // It's an email, find the corresponding username
            user = userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + usernameOrEmail));
            username = user.getUsername();
        } else {
            // It's a username, check if it exists
            if (!userRepository.existsByUsername(usernameOrEmail)) {
                throw new UsernameNotFoundException("User not found with username: " + usernameOrEmail);
            }
            username = usernameOrEmail;
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }

        try {
            // Authenticate with the username
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token with user ID
            String jwt = tokenProvider.generateToken(authentication, user.getUserId());

            log.info("User authenticated successfully: {}", username);

            return new UserDto.JwtResponse(jwt, "Bearer", mapUserToResponse(user));

        } catch (BadCredentialsException e) {
            log.error("Authentication failed for user: {}", username);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    public UserDto.UserResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return mapUserToResponse(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    private UserDto.UserResponse mapUserToResponse(User user) {
        return new UserDto.UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}