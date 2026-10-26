package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.AssetDto;
import com.gaming.agricul.dto.CreateAssetRequest;
import com.gaming.agricul.dto.DisposeAssetRequest;
import com.gaming.agricul.mapper.AssetMapper;
import com.gaming.agricul.service.AssetService;
import com.gaming.agricul.service.ProjectClosureService;
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
@Tag(name = "Assets", description = "Physical goods (land, farm, machines...) brought to or used by a project")
public class AssetController {

    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final ProjectClosureService projectClosureService;

    @PostMapping("/projects/{projectId}/assets")
    @Operation(summary = "Register a new asset for a project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset registered",
                    content = @Content(schema = @Schema(implementation = AssetDto.class))),
            @ApiResponse(responseCode = "404", description = "Project or investor not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid value", content = @Content)
    })
    public ResponseEntity<AssetDto> create(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId,
            @RequestBody CreateAssetRequest request) {
        var asset = assetService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(assetMapper.toDto(asset));
    }

    @GetMapping("/projects/{projectId}/assets")
    @Operation(summary = "List all assets registered for a project")
    @ApiResponse(responseCode = "200", description = "List of assets",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetDto.class))))
    public ResponseEntity<List<AssetDto>> findByProject(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(assetService.findByProject(projectId).stream().map(assetMapper::toDto).toList());
    }

    @GetMapping("/assets/{id}")
    @Operation(summary = "Find an asset by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset found",
                    content = @Content(schema = @Schema(implementation = AssetDto.class))),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content)
    })
    public ResponseEntity<AssetDto> findById(
            @Parameter(description = "Asset identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(assetMapper.toDto(assetService.findById(id)));
    }

    @PostMapping("/assets/{id}/dispose")
    @Operation(summary = "Settle a capital asset once its project is closed (sold, or bought out by an investor)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset settled",
                    content = @Content(schema = @Schema(implementation = AssetDto.class))),
            @ApiResponse(responseCode = "404", description = "Asset or project not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Project not closed, asset not contributed to capital, or already settled", content = @Content)
    })
    public ResponseEntity<AssetDto> dispose(
            @Parameter(description = "Asset identifier", required = true) @PathVariable Long id,
            @RequestBody DisposeAssetRequest request) {
        var asset = projectClosureService.disposeAsset(id, request.disposition(), request.amount(), request.buyerInvestorId());
        return ResponseEntity.ok(assetMapper.toDto(asset));
    }
}
