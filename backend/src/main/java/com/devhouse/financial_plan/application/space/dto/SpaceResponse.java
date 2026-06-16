package com.devhouse.financial_plan.application.space.dto;

import java.time.Instant;

public record SpaceResponse(Long id, String name, String description, Instant createdDate, String currentUserRoleName) {}
