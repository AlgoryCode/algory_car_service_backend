package com.algorycarservice.leave.dto;

/** Annual leave of one person in one year; {@code pending} days are requested but not yet approved. */
public record LeaveBalanceResponse(int year, int allowance, int used, int pending, int remaining) {
}
