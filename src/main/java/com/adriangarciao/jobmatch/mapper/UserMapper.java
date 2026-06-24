package com.adriangarciao.jobmatch.mapper;

import com.adriangarciao.jobmatch.dto.UserCreateDTO;
import com.adriangarciao.jobmatch.dto.UserDTO;
import com.adriangarciao.jobmatch.dto.UserUpdateDTO;
import com.adriangarciao.jobmatch.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "resumes", ignore = true)
    @Mapping(target = "role", ignore = true)
    User fromCreateDto(UserCreateDTO dto);


    // partial updates, ignore nulls
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "resumes", ignore = true)
    @Mapping(target = "role", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);
}
