package com.adriangarciao.jobmatch.dto;

import java.util.List;


public record UserDTO(
        Long id,
        String name,
        String email,
        int age,
        int gradYear,
        String college,
        String major,
        List<String> desiredJobTitle,
        List<String> skills,
        List<String> experience
) {

}
