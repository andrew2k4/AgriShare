package com.gaming.agricul.Controller;

import com.gaming.agricul.Role;
import com.gaming.agricul.dto.FinancialParameterDto;
import com.gaming.agricul.dto.SetFinancialParameterRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.mapper.FinancialParameterMapper;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.FinancialParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/financial-parameters")
@RequiredArgsConstructor
@Tag(name = "Financial Parameters", description = "Configurable risk/impact coefficients used by the UC calculation (Admin only)")
public class FinancialParameterController {

    private final FinancialParameterService financialParameterService;
    private final FinancialParameterMapper financialParameterMapper;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    @Operation(summary = "List the global default coefficients (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "List of parameters",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FinancialParameterDto.class))))
    public ResponseEntity<List<FinancialParameterDto>> listGlobalDefaults(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(financialParameterService.listGlobalDefaults().stream()
                .map(financialParameterMapper::toDto).toList());
    }

    @PutMapping
    @Operation(summary = "Set (create or override) a coefficient, globally or for one project (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Parameter updated",
            content = @Content(schema = @Schema(implementation = FinancialParameterDto.class)))
    public ResponseEntity<FinancialParameterDto> setParameter(
            @RequestBody SetFinancialParameterRequest request, Authentication authentication) {
        var currentUser = currentUserResolver.resolve(authentication);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("This action is reserved to administrators");
        }
        var saved = financialParameterService.setParameter(
                request.paramKey(), request.projectId(), request.value(), currentUser.getId());
        return ResponseEntity.ok(financialParameterMapper.toDto(saved));
    }

    private void requireAdmin(Authentication authentication) {
        if (currentUserResolver.resolve(authentication).getRole() != Role.ADMIN) {
            throw new BusinessRuleException("This action is reserved to administrators");
        }
    }
}
