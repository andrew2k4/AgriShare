package com.gaming.agricul.service;

import com.gaming.agricul.Investor;
import com.gaming.agricul.LivestockType;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.ProductionDay;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectResources;
import com.gaming.agricul.ProjectType;
import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.repository.InvestorRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProductionDayRepository;
import com.gaming.agricul.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Input rules of the plain create/update services (project, investor, operation, production day). */
@ExtendWith(MockitoExtension.class)
class CrudValidationTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private ProductionDayRepository productionDayRepository;

    private static Project project(String name, ProjectType type, LivestockType livestock, Double budget, Double landSize) {
        Project project = new Project();
        project.setName(name);
        project.setProjectType(type);
        project.setLivestockType(livestock);
        project.setResources(new ProjectResources(budget, landSize, false, "  "));
        return project;
    }

    @Test
    void project_validIsSavedWithTrimmedName() {
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Project saved = new ProjectService(projectRepository)
                .save(project("  Ferme du Sud ", ProjectType.LIVESTOCK, LivestockType.CHICKEN, 5_000_000.0, 1000.0));
        assertThat(saved.getName()).isEqualTo("Ferme du Sud");
        assertThat(saved.getResources().getNotes()).isNull();
    }

    @Test
    void project_agricultureDropsLivestockType() {
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Project saved = new ProjectService(projectRepository)
                .save(project("Maïs", ProjectType.AGRICULTURE, LivestockType.PIG, 5_000_000.0, 1000.0));
        assertThat(saved.getLivestockType()).isNull();
    }

    @Test
    void project_rejectsInvalidInput() {
        ProjectService service = new ProjectService(projectRepository);
        assertThatThrownBy(() -> service.save(project(" ", ProjectType.AGRICULTURE, null, 1.0, 1.0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(project("Porcs", ProjectType.LIVESTOCK, null, 1.0, 1.0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(project("Porcs", ProjectType.AGRICULTURE, null, -5.0, 1.0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(project("Porcs", ProjectType.AGRICULTURE, null, 5.0, null)))
                .isInstanceOf(InvalidInputException.class);
        Project withoutResources = project("Porcs", ProjectType.AGRICULTURE, null, 1.0, 1.0);
        withoutResources.setResources(null);
        assertThatThrownBy(() -> service.save(withoutResources)).isInstanceOf(InvalidInputException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void investor_rejectsBlankNameAndNegativeContribution() {
        InvestorService service = new InvestorService(investorRepository);
        assertThatThrownBy(() -> service.save(new Investor(null, "  ", 0)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(new Investor(null, "Marie", -1)))
                .isInstanceOf(InvalidInputException.class);
        verify(investorRepository, never()).save(any());
    }

    @Test
    void operation_rejectsFutureDateAndNonPositiveAmount() {
        OperationService service = new OperationService(operationRepository);
        LocalDate today = LocalDate.now();
        assertThatThrownBy(() -> service.save(new Operation(null, null, "Vente", today.plusDays(5), 1000, OperationType.INCOME)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(new Operation(null, null, "Vente", today, 0, OperationType.INCOME)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(new Operation(null, null, "Vente", today, 1000, null)))
                .isInstanceOf(InvalidInputException.class);
        verify(operationRepository, never()).save(any());
    }

    @Test
    void productionDay_acceptsZeroButNotNegative() {
        ProductionDayService service = new ProductionDayService(productionDayRepository);
        when(productionDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.save(new ProductionDay(null, LocalDate.now(), 0)).getQuantity()).isZero();
        assertThatThrownBy(() -> service.save(new ProductionDay(null, LocalDate.now(), -3)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.save(new ProductionDay(null, null, 10)))
                .isInstanceOf(InvalidInputException.class);
    }
}
