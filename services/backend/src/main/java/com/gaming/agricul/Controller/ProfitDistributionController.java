package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.dto.ProfitDistributionDto;
import com.gaming.agricul.dto.ProposeDistributionRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.mapper.ProfitDistributionMapper;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.ProfitDistributionService;
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
@Tag(name = "Profit Distributions", description = "Propose and validate profit distributions (two-person workflow)")
public class ProfitDistributionController {

    private final ProfitDistributionService profitDistributionService;
    private final ProfitDistributionMapper profitDistributionMapper;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/projects/{projectId}/distributions")
    @Operation(summary = "Propose a new profit distribution for a project and period (Admin/Manager only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Distribution proposed",
                    content = @Content(schema = @Schema(implementation = ProfitDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid request or caller not authorized", content = @Content)
    })
    public ResponseEntity<ProfitDistributionDto> propose(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId,
            @RequestBody ProposeDistributionRequest request,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var distribution = profitDistributionService.propose(projectId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(profitDistributionMapper.toDto(distribution));
    }

    @PostMapping("/distributions/{id}/validate")
    @Operation(summary = "Validate a proposed distribution (must be a different Admin/Manager than the proposer)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribution validated",
                    content = @Content(schema = @Schema(implementation = ProfitDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Distribution not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already decided, or same person as the proposer", content = @Content)
    })
    public ResponseEntity<ProfitDistributionDto> validate(
            @Parameter(description = "Distribution identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var distribution = profitDistributionService.validate(id, currentUser.getId());
        return ResponseEntity.ok(profitDistributionMapper.toDto(distribution));
    }

    @PostMapping("/distributions/{id}/reject")
    @Operation(summary = "Reject a proposed distribution (must be a different Admin/Manager than the proposer)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribution rejected",
                    content = @Content(schema = @Schema(implementation = ProfitDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Distribution not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already decided, or same person as the proposer", content = @Content)
    })
    public ResponseEntity<ProfitDistributionDto> reject(
            @Parameter(description = "Distribution identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        AppUser currentUser = requireManagerOrAdmin(authentication);
        var distribution = profitDistributionService.reject(id, currentUser.getId());
        return ResponseEntity.ok(profitDistributionMapper.toDto(distribution));
    }

    @GetMapping("/projects/{projectId}/distributions")
    @Operation(summary = "List all distributions (any status) for a project")
    @ApiResponse(responseCode = "200", description = "List of distributions",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfitDistributionDto.class))))
    public ResponseEntity<List<ProfitDistributionDto>> findByProject(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(profitDistributionService.findByProject(projectId).stream()
                .map(profitDistributionMapper::toDto).toList());
    }

    @GetMapping("/distributions/{id}")
    @Operation(summary = "Find a distribution by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribution found",
                    content = @Content(schema = @Schema(implementation = ProfitDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Distribution not found", content = @Content)
    })
    public ResponseEntity<ProfitDistributionDto> findById(
            @Parameter(description = "Distribution identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(profitDistributionMapper.toDto(profitDistributionService.findById(id)));
    }

    private AppUser requireManagerOrAdmin(Authentication authentication) {
        AppUser user = currentUserResolver.resolve(authentication);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new BusinessRuleException("This action is reserved to Admins and Managers");
        }
        return user;
    }
}
