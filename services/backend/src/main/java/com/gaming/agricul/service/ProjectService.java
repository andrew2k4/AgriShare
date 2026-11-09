package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.security.CurrentUserResolver;
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

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Project> findAllByOwner(String ownerUid) {
        return projectRepository.findByOwnerUid(ownerUid);
    }

    /**
     * A project the user may see and change: their own, or any project for an admin. Someone
     * else's project answers like a missing one, so its existence is not revealed.
     */
    @Transactional(readOnly = true)
    public Project findAccessible(Long id, AppUser user) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (user.getRole() != Role.ADMIN && !CurrentUserResolver.ownerKey(user).equals(project.getOwnerUid())) {
            throw notFound(id);
        }
        return project;
    }

    public Project save(Project project) {
        return projectRepository.save(project);
    }

    public Project update(Long id, Project project, AppUser user) {
        Project existing = findAccessible(id, user);
        project.setId(id);
        // Never taken from the request: ownership only comes from creation, and closing a
        // project goes through ProjectClosureService
        project.setOwnerUid(existing.getOwnerUid());
        project.setStatus(existing.getStatus());
        return projectRepository.save(project);
    }

    public void delete(Long id, AppUser user) {
        projectRepository.delete(findAccessible(id, user));
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Project not found with id: " + id);
    }
}
