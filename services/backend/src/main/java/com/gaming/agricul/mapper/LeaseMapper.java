package com.gaming.agricul.mapper;

import com.gaming.agricul.Lease;
import com.gaming.agricul.dto.LeaseDto;
import org.springframework.stereotype.Component;

@Component
public class LeaseMapper {

    public LeaseDto toDto(Lease lease) {
        return new LeaseDto(
                lease.getId(),
                lease.getAssetId(),
                lease.getMonthlyRent(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getStatus()
        );
    }
}
