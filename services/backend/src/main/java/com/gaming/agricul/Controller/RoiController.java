package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.RoiDto;
import com.gaming.agricul.service.RoiService;
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
@RequestMapping("/api/projects/{projectId}/investors/{investorId}/roi")
@RequiredArgsConstructor
@Tag(name = "ROI", description = "Return on investment for an investor's position in a project")
public class RoiController {

    private final RoiService roiService;

    @GetMapping
    @Operation(summary = "Compute an investor's ROI (simple and annualized) on a project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ROI computed",
                    content = @Content(schema = @Schema(implementation = RoiDto.class))),
            @ApiResponse(responseCode = "404", description = "No equity-like contribution found", content = @Content)
    })
    public ResponseEntity<RoiDto> compute(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId,
            @Parameter(description = "Investor identifier", required = true) @PathVariable Long investorId) {
        return ResponseEntity.ok(roiService.compute(projectId, investorId));
    }
}
