package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Project;
import com.gaming.agricul.dto.CreateProjectRequest;
import com.gaming.agricul.dto.ProjectDto;
import com.gaming.agricul.mapper.ProjectMapper;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.ProjectClosureService;
import com.gaming.agricul.service.ProjectService;
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
    private final ProjectClosureService projectClosureService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    @Operation(summary = "Retrieve all projects owned by the authenticated user",
               security = @SecurityRequirement(name = "session"))
    @ApiResponse(responseCode = "200", description = "List of projects",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectDto.class))))
    public ResponseEntity<List<ProjectDto>> findAll(Authentication authentication) {
        String uid = extractUid(authentication);
        return ResponseEntity.ok(projectService.findAllByOwner(uid).stream().map(projectMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a project by its id (own projects only, any project for an admin)",
               security = @SecurityRequirement(name = "session"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project found",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectDto> findById(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(projectMapper.toDto(projectService.findAccessible(id, currentUser(authentication))));
    }

    @PostMapping
    @Operation(summary = "Create a new project linked to the authenticated user",
               security = @SecurityRequirement(name = "session"))
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
    @Operation(summary = "Update a project (own projects only, any project for an admin); owner and status are kept",
               security = @SecurityRequirement(name = "session"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<ProjectDto> update(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id,
            @RequestBody ProjectDto dto, Authentication authentication) {
        return ResponseEntity.ok(projectMapper.toDto(
                projectService.update(id, projectMapper.toEntity(dto), currentUser(authentication))));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a project (own projects only, any project for an admin); its capital assets must then be settled individually (see Assets)",
               security = @SecurityRequirement(name = "session"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project closed",
                    content = @Content(schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already closed", content = @Content)
    })
    public ResponseEntity<ProjectDto> close(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        projectService.findAccessible(id, currentUser(authentication));
        return ResponseEntity.ok(projectMapper.toDto(projectClosureService.close(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (own projects only, any project for an admin)",
               security = @SecurityRequirement(name = "session"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project deleted"),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Project identifier", required = true) @PathVariable Long id,
            Authentication authentication) {
        projectService.delete(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private AppUser currentUser(Authentication authentication) {
        return currentUserResolver.resolve(authentication);
    }

    private String extractUid(Authentication authentication) {
        return CurrentUserResolver.ownerKey(currentUser(authentication));
    }
}
