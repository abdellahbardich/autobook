package com.autobook.userservice.service;
import com.autobook.userservice.dto.UserDto;
import com.autobook.userservice.entity.User;
import com.autobook.userservice.repository.UserRepository;
import com.autobook.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto.RegisterRequest registerRequest;
    private UserDto.LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

        registerRequest = new UserDto.RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        loginRequest = new UserDto.LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password");
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto.UserResponse result = userService.registerUser(registerRequest);

        assertThat(result).isNotNull();
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.registerUser(registerRequest)
        );
        assertEquals("Username is already taken!", exception.getMessage());

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.registerUser(registerRequest)
        );
        assertEquals("Email is already in use!", exception.getMessage());

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_SuccessWithUsername() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class), anyLong())).thenReturn("jwtToken");

        UserDto.JwtResponse result = userService.authenticateUser(loginRequest);

        assertThat(result).isNotNull();
        assertEquals("jwtToken", result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(user.getUserId(), result.getUser().getUserId());
        assertEquals(user.getUsername(), result.getUser().getUsername());
        assertEquals(user.getEmail(), result.getUser().getEmail());

        verify(userRepository).existsByUsername(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication, user.getUserId());
    }

    @Test
    void authenticateUser_SuccessWithEmail() {
        loginRequest.setUsernameOrEmail("test@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class), anyLong())).thenReturn("jwtToken");

        UserDto.JwtResponse result = userService.authenticateUser(loginRequest);

        assertThat(result).isNotNull();
        assertEquals("jwtToken", result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(user.getUserId(), result.getUser().getUserId());
        assertEquals(user.getUsername(), result.getUser().getUsername());
        assertEquals(user.getEmail(), result.getUser().getEmail());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(userRepository, never()).existsByUsername(anyString());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication, user.getUserId());
    }

    @Test
    void authenticateUser_UserNotFoundWithUsername() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () ->
                userService.authenticateUser(loginRequest)
        );
        assertEquals("User not found with username: " + loginRequest.getUsernameOrEmail(), exception.getMessage());

        verify(userRepository).existsByUsername(loginRequest.getUsernameOrEmail());
        verify(userRepository, never()).findByUsername(anyString());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void authenticateUser_UserNotFoundWithEmail() {
        loginRequest.setUsernameOrEmail("test@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () ->
                userService.authenticateUser(loginRequest)
        );
        assertEquals("User not found with email: " + loginRequest.getUsernameOrEmail(), exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void authenticateUser_InvalidCredentials() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () ->
                userService.authenticateUser(loginRequest)
        );
        assertEquals("Invalid credentials", exception.getMessage());

        verify(userRepository).existsByUsername(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(tokenProvider, never()).generateToken(any(Authentication.class), anyLong());
    }

    @Test
    void getUserProfile_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        UserDto.UserResponse result = userService.getUserProfile("testuser");

        assertThat(result).isNotNull();
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserProfile_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () ->
                userService.getUserProfile("testuser")
        );
        assertEquals("User not found with username: testuser", exception.getMessage());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("testuser");

        assertThat(result).isNotNull();
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () ->
                userService.getUserByUsername("testuser")
        );
        assertEquals("User not found with username: testuser", exception.getMessage());

        verify(userRepository).findByUsername("testuser");
    }
}