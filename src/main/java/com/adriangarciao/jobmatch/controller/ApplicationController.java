package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.ApplicationCreateDTO;
import com.adriangarciao.jobmatch.dto.ApplicationDTO;
import com.adriangarciao.jobmatch.dto.PagedResponse;
import com.adriangarciao.jobmatch.model.ApplicationStatus;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.UserRepository;
import com.adriangarciao.jobmatch.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(ApplicationService applicationService, UserRepository userRepository) {
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    // ---------- helpers ----------
    private Long currentUserId(Authentication auth) {
        // principal is email per your JwtAuthFilter
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private static final Set<String> ALLOWED_SORTS =
            Set.of("appliedDate", "company", "status", "compensation", "updatedAt", "createdAt", "id");

    private Sort safeSort(String sortBy, String direction) {
        String field = ALLOWED_SORTS.contains(sortBy) ? sortBy : "appliedDate";
        boolean asc = "ASC".equalsIgnoreCase(direction);
        return asc ? Sort.by(field).ascending() : Sort.by(field).descending();
    }

    // ---------- create (mine) ----------
    @PostMapping
    public ResponseEntity<ApplicationDTO> createMyApplication(@Valid @RequestBody ApplicationCreateDTO dto,
                                                              Authentication auth) {
        log.info("Create application endpoint called for user: {}", auth.getName());
        Long userId = currentUserId(auth);
        ApplicationDTO created = applicationService.createForUser(userId, dto);
        return ResponseEntity.ok(created);
    }

    // ---------- get one (mine) ----------
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getMyApplication(@PathVariable Long id, Authentication auth) {
        Long userId = currentUserId(auth);
        ApplicationDTO app = applicationService.getOwned(id, userId);
        return ResponseEntity.ok(app);
    }

    // ---------- list mine (paged) ----------
    @GetMapping("/me/paged")
    public ResponseEntity<Page<ApplicationDTO>> getMyApplicationsPaged(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Long userId = currentUserId(auth);
        Pageable pageable = PageRequest.of(page, size, safeSort(sortBy, direction));
        Page<ApplicationDTO> apps = applicationService.getApplicationsByUser(userId, pageable);
        return ResponseEntity.ok(apps);
    }

    // ---------- search mine (filters) ----------
    @GetMapping("/me/search")
    public ResponseEntity<PagedResponse<ApplicationDTO>> searchMyApplications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            // Multi-value lists (supports repeated params or comma-separated)
            @RequestParam(required = false) List<ApplicationStatus> status,  // e.g. APPLIED,INTERVIEW
            @RequestParam(required = false) List<String> companyList,        // exact matches

            // Single fuzzy filter (contains)
            @RequestParam(required = false) String company,

            // Ranges
            @RequestParam(required = false) Integer minComp,
            @RequestParam(required = false) Integer maxComp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            // Sorting
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Long userId = currentUserId(auth);
        Pageable pageable = PageRequest.of(page, size, safeSort(sortBy, direction));

        PagedResponse<ApplicationDTO> result =
                applicationService.searchApplications(
                        userId, pageable, status, companyList, company,
                        minComp, maxComp, fromDate, toDate);

        return ResponseEntity.ok(result);
    }

    // ---------- update (mine) ----------
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDTO> updateMyApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationDTO dto,
            Authentication auth) {

        Long userId = currentUserId(auth);
        ApplicationDTO updated = applicationService.updateOwned(id, userId, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationDTO> patchMyApplication(
            @PathVariable Long id,
            @RequestBody Map<String, Object> patch, // or a specific Patch DTO if you prefer
            Authentication auth) {

        Long userId = currentUserId(auth);
        ApplicationDTO updated = applicationService.patchOwned(id, userId, patch);
        return ResponseEntity.ok(updated);
    }

    // ---------- delete (mine) ----------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyApplication(@PathVariable Long id, Authentication auth) {
        Long userId = currentUserId(auth);
        applicationService.deleteOwned(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ---------- OPTIONAL: admin-only queries by user ----------
    @GetMapping("/admin/user/{userId}/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ApplicationDTO>> adminGetByUserPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Pageable pageable = PageRequest.of(page, size, safeSort(sortBy, direction));
        Page<ApplicationDTO> apps = applicationService.getApplicationsByUser(userId, pageable);
        return ResponseEntity.ok(apps);
    }
}

