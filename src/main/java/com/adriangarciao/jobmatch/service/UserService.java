package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.UserCreateDTO;
import com.adriangarciao.jobmatch.dto.UserDTO;
import com.adriangarciao.jobmatch.dto.UserUpdateDTO;
import com.adriangarciao.jobmatch.exception.EmailAlreadyUsedException;
import com.adriangarciao.jobmatch.exception.ForbiddenOperationException;
import com.adriangarciao.jobmatch.exception.UserNotFoundException;
import com.adriangarciao.jobmatch.mapper.UserMapper;
import com.adriangarciao.jobmatch.model.Role;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Helpers ----------

    private void assertEmailUnique(String email, Long excludeUserId) {
        if (email == null || email.isBlank()) return;
        userRepository.findByEmail(email)
                .filter(u -> !u.getId().equals(excludeUserId))
                .ifPresent(u -> { throw new EmailAlreadyUsedException(email); });
    }

    private User getOr404(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    // ---------- Deletes ----------

    @Transactional
    public void deleteUserById(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!userRepository.existsById(id)) throw new UserNotFoundException(id);
        userRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    // ---------- Admin create (used by /api/users for admins only) ----------
    // Regular end-user signup should still go through /api/auth/register
    @Transactional
    public UserDTO createUser(UserCreateDTO dto) {
        // Admin path: allow setting role if DTO has it; otherwise default USER
        // If your UserCreateDTO doesn’t carry role, remove the role logic here.
        assertEmailUnique(dto.email(), null);

        User user = userMapper.fromCreateDto(dto);
        if (user.getRole() == null) user.setRole(Role.USER); // safety default
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    // ---------- Reads ----------

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return userMapper.toDto(getOr404(id));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    // ---------- Updates ----------

    /**
     * Admin update – can change everything (including role and email), with uniqueness enforced.
     */
    @Transactional
    public UserDTO adminUpdateUser(Long id, UserUpdateDTO dto) {
        User user = getOr404(id);

        // email uniqueness if changing
        if (dto.email() != null && !dto.email().equalsIgnoreCase(user.getEmail())) {
            assertEmailUnique(dto.email(), id);
        }

        // MapStruct merges allowed fields
        userMapper.updateUserFromDto(dto, user);

        User updated = userRepository.save(user);
        return userMapper.toDto(updated);
    }

    /**
     * Self update – limited fields; cannot elevate role and (optionally) cannot change email.
     * If you want to allow email change for self, keep the uniqueness check and assignment.
     * If you want to forbid, we just ignore email in the DTO (or you could throw).
     */
    @Transactional
    public UserDTO selfUpdateUser(Long id, UserUpdateDTO dto) {
        User user = getOr404(id);

        // Forbid role modifications by non-admin (ignore role in DTO)
        // For email: choose one policy:
        boolean allowSelfChangeEmail = false;

        // Manual selective update to avoid accidentally changing privileged fields:
        if (dto.name() != null)      user.setName(dto.name());
        if (dto.age() != null)       user.setAge(dto.age());
        if (dto.gradYear() != null)  user.setGradYear(dto.gradYear());
        if (dto.college() != null)   user.setCollege(dto.college());
        if (dto.major() != null)     user.setMajor(dto.major());
        if (dto.desiredJobTitle() != null) user.setDesiredJobTitle(dto.desiredJobTitle());
        if (dto.skills() != null)    user.setSkills(dto.skills());
        if (dto.experience() != null) user.setExperience(dto.experience());

        // Email policy
        if (allowSelfChangeEmail && dto.email() != null && !dto.email().equalsIgnoreCase(user.getEmail())) {
            assertEmailUnique(dto.email(), id);
            user.setEmail(dto.email());
        }
        // Never touch role from self updates

        User updated = userRepository.save(user);
        return userMapper.toDto(updated);
    }

    /**
     * Legacy method used by your controller before the split.
     * You can route to admin/self versions from the controller based on principal.role().
     */
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateDTO dto) {
        // If you keep using this, it behaves like adminUpdate. Prefer the two explicit methods above.
        return adminUpdateUser(id, dto);
    }

    @Transactional
    public void changeOwnPassword(String email, String current, String next) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        if (!passwordEncoder.matches(current, u.getPasswordHash())) {
            throw new ForbiddenOperationException("Current password is incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(next));
        userRepository.save(u);
    }
}