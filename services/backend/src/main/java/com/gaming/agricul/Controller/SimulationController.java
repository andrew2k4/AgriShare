package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.ProjectUsageSimulationDto;
import com.gaming.agricul.service.SimulationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/projects/{projectId}/simulate")
@RequiredArgsConstructor
@Tag(name = "Simulation", description = "\"Everyone pays rent\" asset usage simulation (doc §9.4)")
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping
    @Operation(summary = "Simulate the hypothetical monthly cost of using every asset as if it were rented")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation computed",
                    content = @Content(schema = @Schema(implementation = ProjectUsageSimulationDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectUsageSimulationDto> simulate(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId,
            @Parameter(description = "Monthly rate to apply to each asset's value, as a percentage (default 1.0)")
            @RequestParam(required = false) BigDecimal monthlyRatePercent) {
        return ResponseEntity.ok(simulationService.simulateAssetUsage(projectId, monthlyRatePercent));
    }
}
