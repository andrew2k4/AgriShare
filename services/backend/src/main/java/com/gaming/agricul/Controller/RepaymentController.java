package com.gaming.agricul.Controller;

import com.gaming.agricul.dto.RepaymentDto;
import com.gaming.agricul.dto.ScheduleRepaymentRequest;
import com.gaming.agricul.mapper.RepaymentMapper;
import com.gaming.agricul.service.RepaymentService;
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
@Tag(name = "Repayments", description = "Repayments of LOAN/TEMPORARY_INVESTMENT contributions")
public class RepaymentController {

    private final RepaymentService repaymentService;
    private final RepaymentMapper repaymentMapper;

    @PostMapping("/contributions/{contributionId}/repayments")
    @Operation(summary = "Schedule a repayment for a LOAN or TEMPORARY_INVESTMENT contribution")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Repayment scheduled",
                    content = @Content(schema = @Schema(implementation = RepaymentDto.class))),
            @ApiResponse(responseCode = "404", description = "Contribution not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid request or contribution is equity-like", content = @Content)
    })
    public ResponseEntity<RepaymentDto> schedule(
            @Parameter(description = "Contribution identifier", required = true) @PathVariable Long contributionId,
            @RequestBody ScheduleRepaymentRequest request) {
        var repayment = repaymentService.schedule(
                contributionId, request.principalAmount(), request.interestAmount(), request.dueDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(repaymentMapper.toDto(repayment));
    }

    @GetMapping("/contributions/{contributionId}/repayments")
    @Operation(summary = "List all repayments for a contribution")
    @ApiResponse(responseCode = "200", description = "List of repayments",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RepaymentDto.class))))
    public ResponseEntity<List<RepaymentDto>> findByContribution(
            @Parameter(description = "Contribution identifier", required = true) @PathVariable Long contributionId) {
        return ResponseEntity.ok(repaymentService.findByContribution(contributionId).stream()
                .map(repaymentMapper::toDto).toList());
    }

    @PostMapping("/repayments/{id}/pay")
    @Operation(summary = "Mark a repayment as paid (posts an expense operation on the project)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repayment paid",
                    content = @Content(schema = @Schema(implementation = RepaymentDto.class))),
            @ApiResponse(responseCode = "404", description = "Repayment not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already paid", content = @Content)
    })
    public ResponseEntity<RepaymentDto> markPaid(
            @Parameter(description = "Repayment identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(repaymentMapper.toDto(repaymentService.markPaid(id)));
    }
}
