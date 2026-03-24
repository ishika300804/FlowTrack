package com.example.IMS.service;

import com.example.IMS.dto.UserRegistrationDto;
import com.example.IMS.model.User;
import java.util.List;

public interface IUserService {
    User registerUser(UserRegistrationDto registrationDto);
    User registerUserWithRole(UserRegistrationDto registrationDto, String roleName);
    void saveRegistrationHints(Long userId, String businessName, String businessType,
                               String gstHint, String phone, String address);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> getAllUsers();
    User updateUser(Long id, UserRegistrationDto userDto);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
