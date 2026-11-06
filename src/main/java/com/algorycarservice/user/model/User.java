package com.algorycarservice.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** An account: workshop staff ({@link Role#ADMIN}, {@link Role#STAFF}) or an OtoViser customer or provider. */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    public static final int DEFAULT_ANNUAL_LEAVE_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Business (servis işletmesi) the user works for or owns: created at first setup and inherited by staff
     * accounts; for an OtoViser provider it is the listed business's UUID. Customers have none.
     */
    @Column(updatable = false)
    private UUID businessUuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    /** Job title shown in lists, e.g. "Usta" or "Kaportacı". */
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private int annualLeaveDays = DEFAULT_ANNUAL_LEAVE_DAYS;

    /** Former staff stay for history (work orders, leaves) but can no longer sign in. */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
