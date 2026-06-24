package com.adriangarciao.jobmatch.mapper;

import com.adriangarciao.jobmatch.dto.ResumeDTO;
import com.adriangarciao.jobmatch.model.Resume;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

    @Mapping(source = "user.id", target = "userId")
    ResumeDTO toDto(Resume resume);
}
