package com.devhouse.financial_plan.application.user.dto;

import java.time.Instant;

public record CreateUserRequest(
        String name,
        String ministry,
        String nickname,
        String profilePhoto,
        String observation,
        Instant birthdate,
        String email,
        String phoneNumber,
        String genre,
        String maritalStatus
) {}
