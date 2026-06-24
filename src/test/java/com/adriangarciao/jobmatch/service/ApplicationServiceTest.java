package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.ApplicationCreateDTO;
import com.adriangarciao.jobmatch.dto.ApplicationDTO;
import com.adriangarciao.jobmatch.exception.ApplicationNotFoundException;
import com.adriangarciao.jobmatch.exception.ForbiddenOperationException;
import com.adriangarciao.jobmatch.exception.UserNotFoundException;
import com.adriangarciao.jobmatch.mapper.ApplicationMapper;
import com.adriangarciao.jobmatch.model.Application;
import com.adriangarciao.jobmatch.model.ApplicationStatus;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ApplicationRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private User user;
    @Mock
    private Application application;
    @Mock
    private ApplicationCreateDTO createDTO;
    @Mock
    private ApplicationDTO applicationDTO;
    @Mock
    private Pageable pageable;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ApplicationService(applicationRepository, userRepository, applicationMapper);
    }

    @Test
    void createForUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(applicationMapper.fromCreateDto(createDTO)).thenReturn(application);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(applicationDTO);
        ApplicationDTO result = service.createForUser(1L, createDTO);
        assertEquals(applicationDTO, result);
        verify(application).setUser(user);
    }

    @Test
    void createForUser_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.createForUser(1L, createDTO));
    }

    @Test
    void getOwned_success() {
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(application));
        when(application.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(applicationMapper.toDto(application)).thenReturn(applicationDTO);
        ApplicationDTO result = service.getOwned(2L, 1L);
        assertEquals(applicationDTO, result);
    }

    @Test
    void getOwned_notFound() {
        when(applicationRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ApplicationNotFoundException.class, () -> service.getOwned(2L, 1L));
    }

    @Test
    void getOwned_forbidden() {
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(application));
        when(application.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(99L);
        assertThrows(ForbiddenOperationException.class, () -> service.getOwned(2L, 1L));
    }

    @Test
    void getApplicationsByUser_returnsPage() {
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));
        when(applicationRepository.findByUserId(1L, pageable)).thenReturn(page);
        when(applicationMapper.toDto(application)).thenReturn(applicationDTO);
        Page<ApplicationDTO> result = service.getApplicationsByUser(1L, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(applicationDTO, result.getContent().get(0));
    }
}
