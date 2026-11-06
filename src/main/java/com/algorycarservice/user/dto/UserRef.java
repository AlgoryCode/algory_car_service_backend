package com.algorycarservice.user.dto;

import com.algorycarservice.user.model.User;

/** Just enough of a person to show their name next to a record. */
public record UserRef(Long id, String fullName) {

    public static UserRef from(User user) {
        return user == null ? null : new UserRef(user.getId(), user.getFullName());
    }
}
