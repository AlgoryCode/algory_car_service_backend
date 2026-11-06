package com.algorycarservice.user.repository;

import com.algorycarservice.user.model.Role;
import com.algorycarservice.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRoleInOrderByActiveDescFullNameAsc(Collection<Role> roles);

    long countByRoleIn(Collection<Role> roles);
}
