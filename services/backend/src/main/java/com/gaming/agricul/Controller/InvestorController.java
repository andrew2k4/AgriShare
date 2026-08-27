package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.InvestorDto;
import com.gaming.agricul.mapper.InvestorMapper;
import com.gaming.agricul.service.InvestorService;
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
@RequestMapping("/api/investors")
@RequiredArgsConstructor
@Tag(name = "Investors", description = "Management of project investors")
public class InvestorController {

    private final InvestorService investorService;
    private final InvestorMapper investorMapper;

    @GetMapping
    @Operation(summary = "Retrieve all investors")
    @ApiResponse(responseCode = "200", description = "List of investors",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvestorDto.class))))
    public ResponseEntity<List<InvestorDto>> findAll() {
        return ResponseEntity.ok(investorService.findAll().stream().map(investorMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find an investor by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investor found",
                    content = @Content(schema = @Schema(implementation = InvestorDto.class))),
            @ApiResponse(responseCode = "404", description = "Investor not found", content = @Content)
    })
    public ResponseEntity<InvestorDto> findById(
            @Parameter(description = "Investor identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(investorMapper.toDto(investorService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new investor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Investor created",
                    content = @Content(schema = @Schema(implementation = InvestorDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<InvestorDto> save(@RequestBody InvestorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investorMapper.toDto(investorService.save(investorMapper.toEntity(dto))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing investor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investor updated",
                    content = @Content(schema = @Schema(implementation = InvestorDto.class))),
            @ApiResponse(responseCode = "404", description = "Investor not found", content = @Content)
    })
    public ResponseEntity<InvestorDto> update(
            @Parameter(description = "Investor identifier", required = true) @PathVariable Long id,
            @RequestBody InvestorDto dto) {
        return ResponseEntity.ok(investorMapper.toDto(investorService.update(id, investorMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an investor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Investor deleted"),
            @ApiResponse(responseCode = "404", description = "Investor not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Investor identifier", required = true) @PathVariable Long id) {
        investorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
