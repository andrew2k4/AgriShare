package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.ContributionDto;
import com.gaming.agricul.dto.CreateContributionRequest;
import com.gaming.agricul.mapper.ContributionMapper;
import com.gaming.agricul.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Contributions", description = "Investor contributions to a project, with entry valuation")
public class ContributionController {

    private final ContributionService contributionService;
    private final ContributionMapper contributionMapper;

    @PostMapping("/projects/{projectId}/contributions")
    @Operation(summary = "Record a new investor contribution to a project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contribution recorded",
                    content = @Content(schema = @Schema(implementation = ContributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Project or investor not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid contribution (e.g. non-positive amount)", content = @Content)
    })
    public ResponseEntity<ContributionDto> create(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId,
            @RequestBody CreateContributionRequest request) {
        var contribution = contributionService.addContribution(
                projectId, request.investorId(), request.type(), request.amount(), request.entryDate(), request.assetId());
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionMapper.toDto(contribution));
    }

    @GetMapping("/projects/{projectId}/contributions")
    @Operation(summary = "List all contributions recorded for a project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of contributions",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContributionDto.class)))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<List<ContributionDto>> findByProject(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(contributionService.findByProject(projectId).stream()
                .map(contributionMapper::toDto).toList());
    }

    @GetMapping("/investors/{investorId}/contributions")
    @Operation(summary = "List all contributions made by an investor across every project (global view)")
    @ApiResponse(responseCode = "200", description = "List of contributions",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContributionDto.class))))
    public ResponseEntity<List<ContributionDto>> findByInvestor(
            @Parameter(description = "Investor identifier", required = true) @PathVariable Long investorId) {
        return ResponseEntity.ok(contributionService.findByInvestor(investorId).stream()
                .map(contributionMapper::toDto).toList());
    }
}
