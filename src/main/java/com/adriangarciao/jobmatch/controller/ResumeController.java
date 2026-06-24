package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.ResumeDTO;
import com.adriangarciao.jobmatch.dto.ResumeUploadResponse;
import com.adriangarciao.jobmatch.service.ResumeService;
import com.adriangarciao.jobmatch.service.ai.ParserService;
import com.adriangarciao.jobmatch.dto.ResumeParseResponse;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;
    private final ParserService parserService;

    public ResumeController(ResumeService resumeService, ParserService parserService) {
        this.resumeService = resumeService;
        this.parserService = parserService;
    }

    /**
     * Upload resume for the current authenticated user.
     * No userId in the request body/query anymore.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeDTO> upload(
            @RequestPart("file") @NotNull MultipartFile file,
            Authentication auth
    ) {
        log.info("Resume upload endpoint called for user: {}", auth.getName());
        // principal is email per your JwtAuthFilter
        String email = auth.getName();
        ResumeDTO saved = resumeService.storeForEmail(file, email);

        // 201 Created + Location: /api/resumes/{id}
        return ResponseEntity.created(URI.create("/api/resumes/" + saved.id()))
                .body(saved);
    }

    /**
     * Extract text from PDF/DOCX server-side and return parsed resume DTO fragment.
     * Endpoint expects authenticated user (Authentication available) but does not store the file.
     */
    @PostMapping(path = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeParseResponse> parseFile(
            @RequestPart("file") @NotNull MultipartFile file,
            Authentication auth
    ) {
        // basic validation
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String text = "";
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String lower = filename.toLowerCase();

        try (InputStream in = file.getInputStream()) {
            // Use Apache Tika to extract text from the uploaded file (PDF, DOCX, etc.)
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(in, handler, metadata, new ParseContext());
            text = handler.toString();
        } catch (Exception e) {
            log.error("Failed to extract text from uploaded file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }

        var parsed = parserService.parseResume(text == null ? "" : text);
        var resp = new ResumeParseResponse(parsed.rawText(), parsed.skills(), parsed.experiences());
        return ResponseEntity.ok(resp);
    }

    /**
     * Get a single resume by id (owner or admin).
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authzService.ownsResume(authentication, #id) or hasRole('ADMIN')")
    public ResponseEntity<ResumeDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(resumeService.get(id));
    }

    /**
     * List *my* resumes (authenticated user).
     */
    @GetMapping
    public ResponseEntity<List<ResumeDTO>> listMine(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(resumeService.listByEmail(email));
    }

    /**
     * OPTIONAL: Admin-only—list resumes for a given userId.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResumeDTO>> listByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(resumeService.listByUserId(userId));
    }

    /**
     * Download (owner or admin).
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("@authzService.ownsResume(authentication, #id) or hasRole('ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return resumeService.download(id);
    }

    /**
     * Delete (owner or admin).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authzService.ownsResume(authentication, #id) or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}