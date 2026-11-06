package com.algorycarservice.user.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * ADMIN runs the workshop: finance, staff accounts and leave approvals. STAFF works on vehicles and requests leave.
 * CUSTOMER and PROVIDER are OtoViser accounts: a customer books services, a provider owns a listed business.
 * They never see the workshop's own data.
 */
public enum Role {
    ADMIN,
    STAFF,
    CUSTOMER,
    PROVIDER;

    /** Accounts of the workshop itself, as opposed to marketplace accounts. */
    public static final Set<Role> WORKSHOP = EnumSet.of(ADMIN, STAFF);

    public boolean isWorkshop() {
        return WORKSHOP.contains(this);
    }
}
