package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.dto.AppUserDto;
import com.gaming.agricul.dto.UpdateRoleRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.mapper.AppUserMapper;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.AppUserService;
import com.gaming.agricul.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Application users and role assignment")
public class AppUserController {

    private final AppUserService appUserService;
    private final AppUserMapper appUserMapper;
    private final CurrentUserResolver currentUserResolver;
    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "Retrieve all application users (Admin only)",
               security = @SecurityRequirement(name = "session"))
    @ApiResponse(responseCode = "200", description = "List of users",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AppUserDto.class))))
    public ResponseEntity<List<AppUserDto>> findAll(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(appUserService.findAll().stream().map(appUserMapper::toDto).toList());
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Assign a role to a user (Admin only)",
               security = @SecurityRequirement(name = "session"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated",
                    content = @Content(schema = @Schema(implementation = AppUserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Caller is not an Admin", content = @Content)
    })
    public ResponseEntity<AppUserDto> updateRole(
            @Parameter(description = "User identifier", required = true) @PathVariable Long id,
            @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(appUserMapper.toDto(appUserService.updateRole(id, request.role())));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a user: they can no longer sign in and all their sessions end (Admin only)",
               security = @SecurityRequirement(name = "session"))
    public ResponseEntity<AppUserDto> suspend(
            @Parameter(description = "User identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        AppUser admin = requireAdmin(authentication);
        if (admin.getId().equals(id)) {
            throw new BusinessRuleException("You cannot suspend your own account");
        }
        AppUser user = appUserService.suspend(id);
        sessionService.revokeAll(id);
        return ResponseEntity.ok(appUserMapper.toDto(user));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Lift a suspension; an account whose e-mail was never confirmed goes back to pending (Admin only)", security = @SecurityRequirement(name = "session"))
    public ResponseEntity<AppUserDto> reactivate(
            @Parameter(description = "User identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(appUserMapper.toDto(appUserService.reactivate(id)));
    }

    private AppUser requireAdmin(Authentication authentication) {
        AppUser user = currentUserResolver.resolve(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("This action is reserved to administrators");
        }
        return user;
    }
}
