package com.devhouse.financial_plan.application.user.dto;

import java.time.Instant;

public record UpdateUserRequest(
        Integer version,
        String name,
        String nickname,
        String profilePhoto,
        String observation,
        Instant birthdate,
        String phoneNumber,
        String genre,
        String maritalStatus
) {}
