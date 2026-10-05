package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.CreateLeaseRequest;
import com.gaming.agricul.dto.LeaseDto;
import com.gaming.agricul.mapper.LeaseMapper;
import com.gaming.agricul.service.LeaseService;
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
@Tag(name = "Leases", description = "Rent arrangements for assets that stay owned by their contributor")
public class LeaseController {

    private final LeaseService leaseService;
    private final LeaseMapper leaseMapper;

    @PostMapping("/assets/{assetId}/leases")
    @Operation(summary = "Lease an asset to its project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lease created",
                    content = @Content(schema = @Schema(implementation = LeaseDto.class))),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Asset already contributed to capital or already leased", content = @Content)
    })
    public ResponseEntity<LeaseDto> create(
            @Parameter(description = "Asset identifier", required = true) @PathVariable Long assetId,
            @RequestBody CreateLeaseRequest request) {
        var lease = leaseService.create(assetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(leaseMapper.toDto(lease));
    }

    @GetMapping("/assets/{assetId}/leases")
    @Operation(summary = "List all leases (any status) for an asset")
    @ApiResponse(responseCode = "200", description = "List of leases",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LeaseDto.class))))
    public ResponseEntity<List<LeaseDto>> findByAsset(
            @Parameter(description = "Asset identifier", required = true) @PathVariable Long assetId) {
        return ResponseEntity.ok(leaseService.findByAsset(assetId).stream().map(leaseMapper::toDto).toList());
    }

    @PostMapping("/leases/{id}/post-rent")
    @Operation(summary = "Post one period's rent as an expense operation on the project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rent posted",
                    content = @Content(schema = @Schema(implementation = LeaseDto.class))),
            @ApiResponse(responseCode = "404", description = "Lease not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Lease is ended", content = @Content)
    })
    public ResponseEntity<LeaseDto> postRent(
            @Parameter(description = "Lease identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(leaseMapper.toDto(leaseService.postRent(id)));
    }

    @PostMapping("/leases/{id}/end")
    @Operation(summary = "End a lease")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lease ended",
                    content = @Content(schema = @Schema(implementation = LeaseDto.class))),
            @ApiResponse(responseCode = "404", description = "Lease not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Lease already ended", content = @Content)
    })
    public ResponseEntity<LeaseDto> end(
            @Parameter(description = "Lease identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(leaseMapper.toDto(leaseService.end(id)));
    }
}
