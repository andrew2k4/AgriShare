package com.gaming.agricul.mapper;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.dto.AppUserDto;
import org.springframework.stereotype.Component;

@Component
public class AppUserMapper {

    public AppUserDto toDto(AppUser user) {
        return new AppUserDto(
                user.getId(),
                user.getFirebaseUid(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
