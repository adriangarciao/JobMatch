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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService users;


    public UserController(UserService users) {
        this.users = users;
    }

    // ---------- ME endpoints (no IDs in the path) ----------

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal AppPrincipal me) {
        log.info("UserController /me endpoint called for user id: {}", me.id());
        return ResponseEntity.ok(users.getUserById(me.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Replace the current user's profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateMe(@AuthenticationPrincipal AppPrincipal me,
                                            @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(me.id(), dto));
    }

    @PatchMapping("/me")
    @Operation(summary = "Partially update the current user's profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> patchMe(@AuthenticationPrincipal AppPrincipal me,
                                           @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(me.id(), dto));
    }

    // ---------- Admin-only operations ----------

    // Typically end-user registration is via /api/auth/register.
    // Keep this as ADMIN-only to create service accounts, test users, etc.
    @PostMapping
    @Operation(summary = "Create a user (admin only)")
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return ResponseEntity.ok(users.createUser(dto));
    }

    @GetMapping
    @Operation(summary = "List all users (admin only)")
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(users.getAllUsers());
    }

    @DeleteMapping
    @Operation(summary = "Delete all users (admin only)")
    @PreAuthorize("@authz.isAdmin(principal)")
    public ResponseEntity<Void> deleteAllUsers() {
        users.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    // ---------- Owner-or-Admin operations by ID ----------

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id (self or admin)")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(users.getUserById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a user by id (self or admin)")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(id, dto));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a user by id (self or admin)")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<UserDTO> patchUser(@PathVariable Long id,
                                             @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(users.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user by id (self or admin)")
    @PreAuthorize("@authz.isSelfOrAdmin(#id, principal)")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        users.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest req,
                                                  Authentication auth) {

        users.changeOwnPassword(auth.getName(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}