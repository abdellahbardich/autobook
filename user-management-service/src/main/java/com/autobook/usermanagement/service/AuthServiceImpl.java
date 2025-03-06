package com.autobook.usermanagement.service;

import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.autobook.usermanagement.config.JwtService;
import com.autobook.usermanagement.dto.AuthRequest;
import com.autobook.usermanagement.dto.AuthResponse;
import com.autobook.usermanagement.dto.UserDto;
import com.autobook.usermanagement.dto.UserRegistrationDto;
import com.autobook.usermanagement.model.User;
import com.autobook.usermanagement.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public AuthResponse login(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        String jwt = jwtService.generateToken(userDetails);

        return createAuthResponseFromUsername(userDetails.getUsername(), jwt);
    }

    @Override
    public AuthResponse register(UserRegistrationDto registrationDto) {
        User user = userService.registerUser(registrationDto);

        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .toArray(String[]::new))
                        .build();

        String jwt = jwtService.generateToken(userDetails);

        return createAuthResponseFromUser(user, jwt);
    }

    private AuthResponse createAuthResponseFromUsername(String username, String jwt) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return createAuthResponseFromUser(user, jwt);
    }

    private AuthResponse createAuthResponseFromUser(User user, String jwt) {
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .token(jwt)
                .build();
    }
}