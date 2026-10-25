package com.gaming.agricul.dto;

import com.gaming.agricul.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to change a user's role")
public record UpdateRoleRequest(

        @Schema(description = "New role to assign", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
        Role role
) {}
