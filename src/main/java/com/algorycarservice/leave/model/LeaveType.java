package com.algorycarservice.leave.model;

/** Only {@link #ANNUAL} days are taken from the yearly allowance. */
public enum LeaveType {
    ANNUAL,
    SICK,
    EXCUSE,
    UNPAID
}
