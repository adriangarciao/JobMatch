package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.ChangePasswordRequest;
import com.adriangarciao.jobmatch.dto.UserCreateDTO;
import com.adriangarciao.jobmatch.dto.UserDTO;
import com.adriangarciao.jobmatch.dto.UserUpdateDTO;
import com.adriangarciao.jobmatch.mapper.UserMapper;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.UserRepository;
import com.adriangarciao.jobmatch.security.AppPrincipal;
import com.adriangarciao.jobmatch.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService users;


    public UserController(UserService users) {
        this.users = users;
    }

    // ---------- ME endpoints (no IDs in the path) ----------

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal AppPrincipal me) {
        log.info("UserController /me endpoint called for user id: {}", me.id());
        return ResponseEntity.ok(users.getUserById(me.id()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateMe(@AuthenticationPrincipal AppPrincipal me,
                                            @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(me.id(), dto));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> patchMe(@AuthenticationPrincipal AppPrincipal me,
                                           @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(me.id(), dto));
    }

    // ---------- Admin-only operations ----------

    // Typically end-user registration is via /api/auth/register.
    // Keep this as ADMIN-only to create service accounts, test users, etc.
    @PostMapping
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return ResponseEntity.ok(users.createUser(dto));
    }

    @GetMapping
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(users.getAllUsers());
    }

    @DeleteMapping
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<Void> deleteAllUsers() {
        users.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    // ---------- Owner-or-Admin operations by ID ----------

    @GetMapping("/{id}")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(users.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(id, dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> patchUser(@PathVariable Long id,
                                             @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        users.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest req,
                                                  Authentication auth) {

        users.changeOwnPassword(auth.getName(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}