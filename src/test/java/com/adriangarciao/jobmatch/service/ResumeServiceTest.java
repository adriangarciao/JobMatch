package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.ResumeDTO;
import com.adriangarciao.jobmatch.dto.ResumeUploadResponse;
import com.adriangarciao.jobmatch.exception.FileStorageException;
import com.adriangarciao.jobmatch.exception.InvalidFileTypeException;
import com.adriangarciao.jobmatch.exception.ResumeNotFoundException;
import com.adriangarciao.jobmatch.exception.UserNotFoundException;
import com.adriangarciao.jobmatch.mapper.ResumeMapper;
import com.adriangarciao.jobmatch.model.Resume;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ResumeRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.util.List;

class ResumeServiceTest {
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private com.adriangarciao.jobmatch.service.ai.ParserService parserService;
    @Mock
    private MultipartFile file;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resumeService = new ResumeService(resumeRepository, userRepository, resumeMapper, parserService, "uploads");
    }

    @Test
    void storeForEmail_throwsIfFileEmpty() {
        when(file.isEmpty()).thenReturn(true);
        assertThrows(FileStorageException.class, () -> resumeService.storeForEmail(file, "test@example.com"));
    }

    @Test
    void storeForEmail_throwsIfInvalidType() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        assertThrows(InvalidFileTypeException.class, () -> resumeService.storeForEmail(file, "test@example.com"));
    }

    @Test
    void storeForEmail_throwsIfUserNotFound() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> resumeService.storeForEmail(file, "test@example.com"));
    }

    @Test
    void get_throwsIfResumeNotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResumeNotFoundException.class, () -> resumeService.get(1L));
    }

    @Test
    void listByEmail_throwsIfUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> resumeService.listByEmail("test@example.com"));
    }

    @Test
    void listByUserId_returnsList() {
        when(resumeRepository.findByUserId(1L)).thenReturn(java.util.Collections.emptyList());
        assertNotNull(resumeService.listByUserId(1L));
    }

    @Test
    void storeForEmail_persistsParsedText() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("resume.pdf");

        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("Skills: java".getBytes()));

        when(resumeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var parsed = new com.adriangarciao.jobmatch.dto.ParsedResumeDTO("Candidate", List.of("java"), List.of("exp"), "Skills: java");
        when(parserService.parseResume(anyString())).thenReturn(parsed);

        when(resumeMapper.toDto(any())).thenReturn(new ResumeDTO(null, 1L, "resume.pdf", "application/pdf", 123L, java.time.LocalDateTime.now(), "Skills: java"));

        ResumeDTO dto = resumeService.storeForEmail(file, "test@example.com");

        verify(resumeRepository, atLeastOnce()).save(any());
        assertEquals("Skills: java", dto.parsedText());
    }
}
