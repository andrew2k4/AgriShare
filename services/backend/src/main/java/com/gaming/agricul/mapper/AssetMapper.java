package com.gaming.agricul.mapper;

import com.gaming.agricul.Asset;
import com.gaming.agricul.dto.AssetDto;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public AssetDto toDto(Asset asset) {
        return new AssetDto(
                asset.getId(),
                asset.getProjectId(),
                asset.getOwnerInvestorId(),
                asset.getType(),
                asset.getDescription(),
                asset.getInitialValue(),
                asset.getCurrentValue(),
                asset.getOwnershipStatus(),
                asset.getDisposition(),
                asset.getDisposalAmount(),
                asset.getDisposalInvestorId()
        );
    }
}
