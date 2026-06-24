package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.ResumeDTO;
import com.adriangarciao.jobmatch.dto.ResumeUploadResponse;
import com.adriangarciao.jobmatch.exception.*;
import com.adriangarciao.jobmatch.mapper.ResumeMapper;
import com.adriangarciao.jobmatch.model.Resume;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ResumeRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResumeService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeMapper resumeMapper;
    private final Path uploadRoot;
    private final com.adriangarciao.jobmatch.service.ai.ParserService parserService;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeMapper resumeMapper,
            com.adriangarciao.jobmatch.service.ai.ParserService parserService,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeMapper = resumeMapper;
        this.uploadRoot = Path.of(uploadDir);
        this.parserService = parserService;
    }

    /** Upload for current user (resolved by email from Authentication). */
    public ResumeDTO storeForEmail(MultipartFile file, String email) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file uploaded or file is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException("Only PDF or DOCX are allowed.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email '%s' not found".formatted(email)));

        String originalName = StringUtils.cleanPath(
                Optional.ofNullable(file.getOriginalFilename()).orElse("resume")
        );
        String ext = resolveExtension(originalName, contentType); // ".pdf" or ".docx"
        String storedName = UUID.randomUUID() + ext;
        Path target = uploadRoot.resolve(storedName);

        try {
            Files.createDirectories(uploadRoot);
            // prevent path traversal
            if (!target.normalize().startsWith(uploadRoot.normalize())) {
                throw new FileStorageException("Invalid path.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file for user {}: {}", email, e.getMessage(), e);
            throw new FileStorageException("Failed to store file.", e);
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setOriginalFilename(originalName);
        resume.setStoredFilename(storedName);
        resume.setContentType(contentType);
        resume.setSizeBytes(file.getSize());
        resume.setUploadedAt(LocalDateTime.now());
        resume.setStoragePath(target.toString());

        Resume saved = resumeRepository.save(resume);

        // Attempt to extract text and persist parsed text (best-effort)
        try (java.io.InputStream in = file.getInputStream()) {
            org.apache.tika.sax.BodyContentHandler handler = new org.apache.tika.sax.BodyContentHandler(-1);
            org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
            org.apache.tika.parser.AutoDetectParser parser = new org.apache.tika.parser.AutoDetectParser();
            parser.parse(in, handler, metadata, new org.apache.tika.parser.ParseContext());
            String extracted = handler.toString();
            var parsed = parserService.parseResume(extracted == null ? "" : extracted);
            saved.setParsedText(parsed.rawText());
            resumeRepository.save(saved);
        } catch (Exception e) {
            log.warn("Failed to parse and persist resume text for {}: {}", email, e.getMessage());
        }

        return resumeMapper.toDto(saved);
    }

    /** Get single resume by id (controller enforces ownership/admin). */
    public ResumeDTO get(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));
        return resumeMapper.toDto(resume);
    }

    /** List resumes for current user (by email). */
    public List<ResumeDTO> listByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email '%s' not found".formatted(email)));
        return resumeRepository.findByUserId(user.getId())
                .stream().map(resumeMapper::toDto).toList();
    }

    /** Admin-only helper: list by userId. */
    public List<ResumeDTO> listByUserId(Long userId) {
        return resumeRepository.findByUserId(userId)
                .stream().map(resumeMapper::toDto).toList();
    }

    /** Download (controller enforces ownership/admin). */
    public ResponseEntity<Resource> download(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));

        Path filePath = Path.of(resume.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new FileStorageException("Stored file is missing: " + resume.getStoredFilename());
        }

        try {
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath, StandardOpenOption.READ));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(resume.getOriginalFilename()).build());
            headers.setContentType(MediaType.parseMediaType(resume.getContentType()));
            headers.setContentLength(Files.size(filePath));
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (IOException e) {
            log.error("Failed to read stored file for resume {}: {}", id, e.getMessage(), e);
            throw new FileStorageException("Failed to read stored file.", e);
        }
    }

    /** Delete (controller enforces ownership/admin). */
    public void delete(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));

        Path filePath = Path.of(resume.getStoragePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file for resume {}: {}", id, e.getMessage());
        }
        resumeRepository.deleteById(id);
    }

    private String resolveExtension(String originalName, String contentType) {
        String lower = originalName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return ".pdf";
        if (lower.endsWith(".docx")) return ".docx";
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> "";
        };
    }
}
