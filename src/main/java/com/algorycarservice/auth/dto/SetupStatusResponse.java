package com.algorycarservice.auth.dto;

/** @param setupRequired no account exists yet, so the app offers to create the first admin */
public record SetupStatusResponse(boolean setupRequired) {
}
