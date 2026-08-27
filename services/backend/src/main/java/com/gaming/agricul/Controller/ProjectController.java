package com.gaming.agricul.Controller;

import com.gaming.agricul.Project;
import com.gaming.agricul.dto.CreateProjectRequest;
import com.gaming.agricul.dto.ProjectDto;
import com.gaming.agricul.mapper.ProjectMapper;
import com.gaming.agricul.service.ProjectService;
import com.google.firebase.auth.FirebaseToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Management of agricultural and livestock projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    @GetMapping
    @Operation(summary = "Retrieve all projects owned by the authenticated user",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "List of projects",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectDto.class))))
    public ResponseEntity<List<ProjectDto>> findAll(Authentication authentication) {
        String uid = extractUid(authentication);
        return ResponseEntity.ok(projectService.findAllByOwner(uid).stream().map(projectMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a project by its id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project found",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectDto> findById(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(projectMapper.toDto(projectService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new project linked to the authenticated user",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<ProjectDto> save(@RequestBody CreateProjectRequest request, Authentication authentication) {
        Project project = projectMapper.fromRequest(request);
        project.setOwnerUid(extractUid(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMapper.toDto(projectService.save(project)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectDto> update(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id,
            @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectMapper.toDto(projectService.update(id, projectMapper.toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project deleted"),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id) {
        projectService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String extractUid(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseToken token) {
            return token.getUid();
        }
        return "dev-user";
    }
}
