package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.CapitalDistributionDto;
import com.gaming.agricul.mapper.CapitalDistributionMapper;
import com.gaming.agricul.service.CapitalDistributionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/capital-distributions")
@RequiredArgsConstructor
@Tag(name = "Capital Distributions", description = "Management of capital distribution among investors")
public class CapitalDistributionController {

    private final CapitalDistributionService capitalDistributionService;
    private final CapitalDistributionMapper capitalDistributionMapper;

    @GetMapping
    @Operation(summary = "Retrieve all capital distributions")
    @ApiResponse(responseCode = "200", description = "List of capital distributions",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CapitalDistributionDto.class))))
    public ResponseEntity<List<CapitalDistributionDto>> findAll() {
        return ResponseEntity.ok(capitalDistributionService.findAll().stream().map(capitalDistributionMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a capital distribution by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capital distribution found",
                    content = @Content(schema = @Schema(implementation = CapitalDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Capital distribution not found", content = @Content)
    })
    public ResponseEntity<CapitalDistributionDto> findById(
            @Parameter(description = "Capital distribution identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(capitalDistributionMapper.toDto(capitalDistributionService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new capital distribution")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Capital distribution created",
                    content = @Content(schema = @Schema(implementation = CapitalDistributionDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<CapitalDistributionDto> save(@RequestBody CapitalDistributionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalDistributionMapper.toDto(capitalDistributionService.save(capitalDistributionMapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing capital distribution")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capital distribution updated",
                    content = @Content(schema = @Schema(implementation = CapitalDistributionDto.class))),
            @ApiResponse(responseCode = "404", description = "Capital distribution not found", content = @Content)
    })
    public ResponseEntity<CapitalDistributionDto> update(
            @Parameter(description = "Capital distribution identifier", required = true) @PathVariable Long id,
            @RequestBody CapitalDistributionDto dto) {
        return ResponseEntity.ok(capitalDistributionMapper.toDto(capitalDistributionService.update(id, capitalDistributionMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a capital distribution")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Capital distribution deleted"),
            @ApiResponse(responseCode = "404", description = "Capital distribution not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Capital distribution identifier", required = true) @PathVariable Long id) {
        capitalDistributionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
