package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.OperationDto;
import com.gaming.agricul.mapper.OperationMapper;
import com.gaming.agricul.service.OperationService;
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
@RequestMapping("/api/operations")
@RequiredArgsConstructor
@Tag(name = "Operations", description = "Management of financial operations (income and expenses)")
public class OperationController {

    private final OperationService operationService;
    private final OperationMapper operationMapper;

    @GetMapping
    @Operation(summary = "Retrieve all operations")
    @ApiResponse(responseCode = "200", description = "List of operations",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OperationDto.class))))
    public ResponseEntity<List<OperationDto>> findAll() {
        return ResponseEntity.ok(operationService.findAll().stream().map(operationMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find an operation by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operation found",
                    content = @Content(schema = @Schema(implementation = OperationDto.class))),
            @ApiResponse(responseCode = "404", description = "Operation not found", content = @Content)
    })
    public ResponseEntity<OperationDto> findById(
            @Parameter(description = "Operation identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(operationMapper.toDto(operationService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new financial operation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operation created",
                    content = @Content(schema = @Schema(implementation = OperationDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<OperationDto> save(@RequestBody OperationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operationMapper.toDto(operationService.save(operationMapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing operation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operation updated",
                    content = @Content(schema = @Schema(implementation = OperationDto.class))),
            @ApiResponse(responseCode = "404", description = "Operation not found", content = @Content)
    })
    public ResponseEntity<OperationDto> update(
            @Parameter(description = "Operation identifier", required = true) @PathVariable Long id,
            @RequestBody OperationDto dto) {
        return ResponseEntity.ok(operationMapper.toDto(operationService.update(id, operationMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an operation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operation deleted"),
            @ApiResponse(responseCode = "404", description = "Operation not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Operation identifier", required = true) @PathVariable Long id) {
        operationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
