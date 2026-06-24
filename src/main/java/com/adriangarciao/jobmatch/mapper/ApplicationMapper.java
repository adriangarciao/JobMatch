package com.adriangarciao.jobmatch.mapper;

import com.adriangarciao.jobmatch.dto.ApplicationCreateDTO;
import com.adriangarciao.jobmatch.dto.ApplicationDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    // Entity -> DTO (lift user.id into userId)
    @Mapping(source = "user.id", target = "userId")
    ApplicationDTO toDto(com.adriangarciao.jobmatch.model.Application application);

    // Create DTO -> Entity (DB generates id; service sets user)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    com.adriangarciao.jobmatch.model.Application fromCreateDto(ApplicationCreateDTO dto);

    // Update: ignore nulls and never touch id via DTO
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // keep association updates explicit in service
    void updateApplicationFromDto(
            ApplicationDTO dto,
            @MappingTarget com.adriangarciao.jobmatch.model.Application application
    );
}