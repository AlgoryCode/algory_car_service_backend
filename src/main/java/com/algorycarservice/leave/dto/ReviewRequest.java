package com.algorycarservice.leave.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(@Size(max = 300, message = "{validation.note.size}") String note) {
}
