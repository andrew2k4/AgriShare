package com.gaming.agricul.service;

import com.gaming.agricul.ProjectResources;
import com.gaming.agricul.ProjectType;
import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.validation.InputRules;
import com.gaming.agricul.Project;
import com.gaming.agricul.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    /** In m²: 100 000 hectares. */
    private static final double MAX_LAND_SIZE = 1_000_000_000;

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Project> findAllByOwner(String ownerUid) {
        return projectRepository.findByOwnerUid(ownerUid);
    }

    @Transactional(readOnly = true)
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
    }

    public Project save(Project project) {
        validate(project, true);
        return projectRepository.save(project);
    }

    public Project update(Long id, Project project) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        validate(project, false);
        project.setId(id);
        return projectRepository.save(project);
    }

    /** Same rules as the "Nouveau projet" form. Normalizes the text fields in place. */
    private static void validate(Project project, boolean creating) {
        project.setName(InputRules.text(project.getName(), "name", InputRules.MAX_TEXT));
        InputRules.required(project.getProjectType(), "projectType");
        if (project.getProjectType() == ProjectType.LIVESTOCK) {
            InputRules.required(project.getLivestockType(), "livestockType");
        } else {
            project.setLivestockType(null);
        }

        ProjectResources resources = project.getResources();
        if (resources == null) {
            if (creating) {
                throw new InvalidInputException("budget is required");
            }
            return;
        }
        InputRules.number(resources.getBudget(), "budget", InputRules.MAX_AMOUNT.doubleValue(), false);
        InputRules.number(resources.getLandSize(), "landSize", MAX_LAND_SIZE, false);
        resources.setNotes(InputRules.optionalText(resources.getNotes(), "notes", InputRules.MAX_TEXT));
    }

    public void deleteById(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }
}
