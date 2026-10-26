package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.ProjectFinancialSummaryDto;
import com.gaming.agricul.service.ProjectFinancialSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/financial-summary")
@RequiredArgsConstructor
@Tag(name = "Financial Summary", description = "Aggregated financial dashboard for a project (doc §19)")
public class ProjectFinancialSummaryController {

    private final ProjectFinancialSummaryService projectFinancialSummaryService;

    @GetMapping
    @Operation(summary = "Compute the financial dashboard indicators for a project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary computed",
                    content = @Content(schema = @Schema(implementation = ProjectFinancialSummaryDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectFinancialSummaryDto> compute(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(projectFinancialSummaryService.compute(projectId));
    }
}
