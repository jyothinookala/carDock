package com.cardock.dto;

import com.cardock.entity.AppUser;
import com.cardock.entity.UserRole;

public record UserResponse(Long id, String fullName, String email, UserRole role) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
