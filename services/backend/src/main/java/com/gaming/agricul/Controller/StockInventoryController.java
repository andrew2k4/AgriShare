package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.StockInventoryDto;
import com.gaming.agricul.mapper.StockInventoryMapper;
import com.gaming.agricul.service.StockInventoryService;
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
@RequestMapping("/api/stock-inventories")
@RequiredArgsConstructor
@Tag(name = "Stock Inventories", description = "Management of material and input stock inventories")
public class  StockInventoryController {

    private final StockInventoryService stockInventoryService;
    private final StockInventoryMapper stockInventoryMapper;

    @GetMapping
    @Operation(summary = "Retrieve all stock inventories")
    @ApiResponse(responseCode = "200", description = "List of stock inventories",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StockInventoryDto.class))))
    public ResponseEntity<List<StockInventoryDto>> findAll() {
        return ResponseEntity.ok(stockInventoryService.findAll().stream().map(stockInventoryMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a stock inventory by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock inventory found",
                    content = @Content(schema = @Schema(implementation = StockInventoryDto.class))),
            @ApiResponse(responseCode = "404", description = "Stock inventory not found", content = @Content)
    })
    public ResponseEntity<StockInventoryDto> findById(
            @Parameter(description = "Stock inventory identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(stockInventoryMapper.toDto(stockInventoryService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new stock inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock inventory created",
                    content = @Content(schema = @Schema(implementation = StockInventoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<StockInventoryDto> save(@RequestBody StockInventoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockInventoryMapper.toDto(stockInventoryService.save(stockInventoryMapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing stock inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock inventory updated",
                    content = @Content(schema = @Schema(implementation = StockInventoryDto.class))),
            @ApiResponse(responseCode = "404", description = "Stock inventory not found", content = @Content)
    })
    public ResponseEntity<StockInventoryDto> update(
            @Parameter(description = "Stock inventory identifier", required = true) @PathVariable Long id,
            @RequestBody StockInventoryDto dto) {
        return ResponseEntity.ok(stockInventoryMapper.toDto(stockInventoryService.update(id, stockInventoryMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a stock inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock inventory deleted"),
            @ApiResponse(responseCode = "404", description = "Stock inventory not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Stock inventory identifier", required = true) @PathVariable Long id) {
        stockInventoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
