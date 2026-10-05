package com.gaming.agricul.mapper;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.dto.ContributionDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionMapperTest {

    private final ContributionMapper mapper = new ContributionMapper();

    @Test
    void sharePercentageAtEntry_matchesDocumentWorkedExample() {
        // Moteur Financier doc, section 4: valeur=20 000 000, investissement=2 000 000 -> ~9,09 %
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("2000000"), LocalDate.of(2026, 1, 15), new BigDecimal("20000000"), null, null);

        ContributionDto dto = mapper.toDto(contribution);

        assertThat(dto.sharePercentageAtEntry()).isEqualByComparingTo("9.09");
    }

    @Test
    void sharePercentageAtEntry_isHundredPercent_forFirstContributionToEmptyProject() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("500000"), LocalDate.now(), BigDecimal.ZERO, null, null);

        ContributionDto dto = mapper.toDto(contribution);

        assertThat(dto.sharePercentageAtEntry()).isEqualByComparingTo("100.00");
    }

    @Test
    void sharePercentageAtEntry_isZero_whenTotalIsZero() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                BigDecimal.ZERO, LocalDate.now(), BigDecimal.ZERO, null, null);

        ContributionDto dto = mapper.toDto(contribution);

        assertThat(dto.sharePercentageAtEntry()).isEqualByComparingTo("0");
    }
}
