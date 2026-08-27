package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.ProductionDayDto;
import com.gaming.agricul.mapper.ProductionDayMapper;
import com.gaming.agricul.service.ProductionDayService;
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
@RequestMapping("/api/production-days")
@RequiredArgsConstructor
@Tag(name = "Production Days", description = "Management of daily production records")
public class ProductionDayController {

    private final ProductionDayService productionDayService;
    private final ProductionDayMapper productionDayMapper;

    @GetMapping
    @Operation(summary = "Retrieve all production days")
    @ApiResponse(responseCode = "200", description = "List of production days",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductionDayDto.class))))
    public ResponseEntity<List<ProductionDayDto>> findAll() {
        return ResponseEntity.ok(productionDayService.findAll().stream().map(productionDayMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a production day by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Production day found",
                    content = @Content(schema = @Schema(implementation = ProductionDayDto.class))),
            @ApiResponse(responseCode = "404", description = "Production day not found", content = @Content)
    })
    public ResponseEntity<ProductionDayDto> findById(
            @Parameter(description = "Production day identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(productionDayMapper.toDto(productionDayService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Record a new production day")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Production day created",
                    content = @Content(schema = @Schema(implementation = ProductionDayDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<ProductionDayDto> save(@RequestBody ProductionDayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionDayMapper.toDto(productionDayService.save(productionDayMapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a production day")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Production day updated",
                    content = @Content(schema = @Schema(implementation = ProductionDayDto.class))),
            @ApiResponse(responseCode = "404", description = "Production day not found", content = @Content)
    })
    public ResponseEntity<ProductionDayDto> update(
            @Parameter(description = "Production day identifier", required = true) @PathVariable Long id,
            @RequestBody ProductionDayDto dto) {
        return ResponseEntity.ok(productionDayMapper.toDto(productionDayService.update(id, productionDayMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a production day")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Production day deleted"),
            @ApiResponse(responseCode = "404", description = "Production day not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Production day identifier", required = true) @PathVariable Long id) {
        productionDayService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
