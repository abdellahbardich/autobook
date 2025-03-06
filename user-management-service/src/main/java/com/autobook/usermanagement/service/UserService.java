package com.autobook.usermanagement.service;

import java.util.List;

import com.autobook.usermanagement.dto.UserDto;
import com.autobook.usermanagement.dto.UserRegistrationDto;
import com.autobook.usermanagement.model.User;

public interface UserService {
    User registerUser(UserRegistrationDto registrationDto);
    UserDto getUserById(Long id);
    UserDto getUserByUsername(String username);
    UserDto getUserByEmail(String email);
    List<UserDto> getAllUsers();
    UserDto updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}