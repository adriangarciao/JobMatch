package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.UserCreateDTO;
import com.adriangarciao.jobmatch.dto.UserDTO;
import com.adriangarciao.jobmatch.dto.UserUpdateDTO;
import com.adriangarciao.jobmatch.exception.UserNotFoundException;
import com.adriangarciao.jobmatch.mapper.UserMapper;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserById_returnsUserDTO_whenUserExists() {
        User user = new User();
        user.setId(1L);
        UserDTO userDTO = new UserDTO(
                1L,
                "Test User",
                "test@example.com",
                25,
                2025,
                "Test College",
                "Computer Science",
                List.of("Developer"),
                List.of("Java", "Spring"),
                List.of("Internship at X")
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user)).thenReturn(Optional.empty());
        when(userMapper.toDto(user)).thenReturn(userDTO);
        UserDTO result = userService.getUserById(1L);
        assertEquals(userDTO, result);
    }

    @Test
    void getUserById_throwsException_whenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(2L));
    }

    @Test
    void getAllUsers_returnsListOfUserDTOs() {
        User user = new User();
        List<User> users = Collections.singletonList(user);
        UserDTO userDTO = new UserDTO(
                1L,
                "Test User",
                "test@example.com",
                25,
                2025,
                "Test College",
                "Computer Science",
                List.of("Developer"),
                List.of("Java", "Spring"),
                List.of("Internship at X")
        );
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDto(user)).thenReturn(userDTO);
        List<UserDTO> result = userService.getAllUsers();
        assertEquals(1, result.size());
        assertEquals(userDTO, result.get(0));
    }

    @Test
    void createUser_savesUser_andReturnsDTO() {
        UserCreateDTO dto = new UserCreateDTO(
                "Test User",
                "test@example.com",
                25,
                2025,
                "Test College",
                "Computer Science",
                List.of("Developer"),
                List.of("Java", "Spring"),
                List.of("Internship at X")
        );
        User user = new User();
        User saved = new User();
        UserDTO userDTO = new UserDTO(
                1L,
                "Test User",
                "test@example.com",
                25,
                2025,
                "Test College",
                "Computer Science",
                List.of("Developer"),
                List.of("Java", "Spring"),
                List.of("Internship at X")
        );
        when(userMapper.fromCreateDto(dto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(userDTO);
        UserDTO result = userService.createUser(dto);
        assertEquals(userDTO, result);
    }

    @Test
    void updateUser_updatesUser_andReturnsDTO() {
        UserUpdateDTO dto = new UserUpdateDTO(
                "Updated Name",
                "updated@example.com",
                30,
                2026,
                "Updated College",
                "Math",
                List.of("Manager"),
                List.of("Python"),
                List.of("Lead at Y")
        );
        User user = new User();
        user.setId(1L);
        User updated = new User();
        UserDTO userDTO = new UserDTO(
                1L,
                "Updated Name",
                "updated@example.com",
                30,
                2026,
                "Updated College",
                "Math",
                List.of("Manager"),
                List.of("Python"),
                List.of("Lead at Y")
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updated);
        when(userMapper.toDto(updated)).thenReturn(userDTO);
        UserDTO result = userService.updateUser(1L, dto);
        assertEquals(userDTO, result);
    }

    @Test
    void deleteUserById_deletesUser() {
        User user = new User();
        user.setId(1L);
        // The service may call findById multiple times, so always return the user
       long userId = 1L;
       when(userRepository.existsById(userId)).thenReturn(true);
       doNothing().when(userRepository).deleteById(userId);
       assertDoesNotThrow(() -> userService.deleteUserById(userId));
       verify(userRepository).deleteById(userId);
    }
}
