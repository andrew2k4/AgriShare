package com.gaming.agricul.mapper;

import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectCapital;
import com.gaming.agricul.ProjectResources;
import com.gaming.agricul.ProjectStatistics;
import com.gaming.agricul.dto.CreateProjectRequest;
import com.gaming.agricul.dto.ProjectCapitalDto;
import com.gaming.agricul.dto.ProjectDto;
import com.gaming.agricul.dto.ProjectResourcesDto;
import com.gaming.agricul.dto.ProjectStatisticsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final ProductionDayMapper productionDayMapper;
    private final OperationMapper operationMapper;

    public ProjectDto toDto(Project project) {
        ProjectResourcesDto resources = null;
        if (project.getResources() != null) {
            ProjectResources r = project.getResources();
            resources = new ProjectResourcesDto(r.getBudget(), r.getLandSize(), r.getExistingStructures(), r.getNotes());
        }

        ProjectCapitalDto capital = null;
        if (project.getCapital() != null) {
            ProjectCapital c = project.getCapital();
            capital = new ProjectCapitalDto(c.getInitialCapital(), c.getCurrentCapital(), c.getTotalInvestment(), c.getTotalExpenses());
        }

        ProjectStatisticsDto statistics = null;
        if (project.getStatistics() != null) {
            ProjectStatistics s = project.getStatistics();
            statistics = new ProjectStatisticsDto(s.getTotalProduction(), s.getTotalRevenue(), s.getTotalProfit(), s.getProductivityRate());
        }

        return new ProjectDto(
                project.getId(),
                project.getOwnerUid(),
                project.getName(),
                project.getProjectType(),
                project.getLivestockType(),
                resources,
                capital,
                statistics,
                project.getProductions().stream().map(productionDayMapper::toDto).toList(),
                project.getOperations().stream().map(operationMapper::toDto).toList()
        );
    }

    public Project fromRequest(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setProjectType(request.projectType());
        project.setLivestockType(request.livestockType());
        if (request.budget() != null || request.landSize() != null) {
            project.setResources(new ProjectResources(
                    request.budget(),
                    request.landSize(),
                    request.existingStructures() != null ? request.existingStructures() : false,
                    request.notes()
            ));
        }
        return project;
    }

    public Project toEntity(ProjectDto dto) {
        Project project = new Project();
        project.setId(dto.id());
        project.setOwnerUid(dto.ownerUid());
        project.setName(dto.name());
        project.setProjectType(dto.projectType());
        project.setLivestockType(dto.livestockType());

        if (dto.resources() != null) {
            ProjectResourcesDto r = dto.resources();
            project.setResources(new ProjectResources(r.budget(), r.landSize(), r.existingStructures(), r.notes()));
        }
        if (dto.capital() != null) {
            ProjectCapitalDto c = dto.capital();
            project.setCapital(new ProjectCapital(c.initialCapital(), c.currentCapital(), c.totalInvestment(), c.totalExpenses()));
        }
        if (dto.statistics() != null) {
            ProjectStatisticsDto s = dto.statistics();
            project.setStatistics(new ProjectStatistics(s.totalProduction(), s.totalRevenue(), s.totalProfit(), s.productivityRate()));
        }

        if (dto.productions() != null) {
            project.setProductions(dto.productions().stream().map(productionDayMapper::toEntity).toList());
        }
        if (dto.operations() != null) {
            project.setOperations(dto.operations().stream().map(operationMapper::toEntity).toList());
        }

        return project;
    }
}
