package com.gaming.agricul;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResources {

    private Double budget;
    private Double landSize;
    private Boolean existingStructures;
    private String notes;
}
