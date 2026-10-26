package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.dto.RequestWithdrawalRequest;
import com.gaming.agricul.dto.WithdrawalDto;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.mapper.WithdrawalMapper;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.WithdrawalService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Withdrawals", description = "Conditional capital withdrawals (two-person workflow)")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalMapper withdrawalMapper;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/contributions/{contributionId}/withdrawals")
    @Operation(summary = "Request a capital withdrawal from a contribution (Admin/Manager only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Withdrawal requested (may be BLOCKED if the project cannot absorb it)",
                    content = @Content(schema = @Schema(implementation = WithdrawalDto.class))),
            @ApiResponse(responseCode = "404", description = "Contribution not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid amount or caller not authorized", content = @Content)
    })
    public ResponseEntity<WithdrawalDto> request(
            @Parameter(description = "Contribution identifier", required = true) @PathVariable Long contributionId,
            @RequestBody RequestWithdrawalRequest request,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var withdrawal = withdrawalService.request(contributionId, request.amount(), currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(withdrawalMapper.toDto(withdrawal));
    }

    @PostMapping("/withdrawals/{id}/validate")
    @Operation(summary = "Validate a requested withdrawal (must be a different Admin/Manager than the requester)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal validated",
                    content = @Content(schema = @Schema(implementation = WithdrawalDto.class))),
            @ApiResponse(responseCode = "404", description = "Withdrawal not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already decided, blocked, or same person as the requester", content = @Content)
    })
    public ResponseEntity<WithdrawalDto> validate(
            @Parameter(description = "Withdrawal identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var withdrawal = withdrawalService.validate(id, currentUser.getId());
        return ResponseEntity.ok(withdrawalMapper.toDto(withdrawal));
    }

    @PostMapping("/withdrawals/{id}/reject")
    @Operation(summary = "Reject a requested withdrawal (must be a different Admin/Manager than the requester)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal rejected",
                    content = @Content(schema = @Schema(implementation = WithdrawalDto.class))),
            @ApiResponse(responseCode = "404", description = "Withdrawal not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already decided, or same person as the requester", content = @Content)
    })
    public ResponseEntity<WithdrawalDto> reject(
            @Parameter(description = "Withdrawal identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var withdrawal = withdrawalService.reject(id, currentUser.getId());
        return ResponseEntity.ok(withdrawalMapper.toDto(withdrawal));
    }

    @GetMapping("/projects/{projectId}/withdrawals")
    @Operation(summary = "List all withdrawal requests (any status) for a project")
    @ApiResponse(responseCode = "200", description = "List of withdrawals",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = WithdrawalDto.class))))
    public ResponseEntity<List<WithdrawalDto>> findByProject(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(withdrawalService.findByProject(projectId).stream()
                .map(withdrawalMapper::toDto).toList());
    }

    private AppUser requireManagerOrAdmin(Authentication authentication) {
        AppUser user = currentUserResolver.resolve(authentication);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new BusinessRuleException("This action is reserved to Admins and Managers");
        }
        return user;
    }
}
