package com.algorycarservice.security;

import com.algorycarservice.user.model.Role;

import java.security.Principal;

/** @param sessionId refresh-token family this access token belongs to */
public record AuthUser(Long id, String email, Role role, String sessionId) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
