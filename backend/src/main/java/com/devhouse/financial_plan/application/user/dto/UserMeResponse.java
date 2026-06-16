package com.devhouse.financial_plan.application.user.dto;

import java.time.Instant;

public record UserMeResponse(
        Long id,
        String name,
        String email,
        String nickname,
        String phoneNumber,
        Instant birthdate,
        String genre,
        String maritalStatus
) {}
