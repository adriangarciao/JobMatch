package com.adriangarciao.jobmatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

// entity
@Entity
@Table(name = "resumes")
@Data
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String originalFilename;
    private String storedFilename;  // e.g., UUID.pdf
    private String contentType;

    @Min(0)
    private Long sizeBytes;

    private LocalDateTime uploadedAt;

    // relative path (e.g., "uploads/uuid.pdf"). You can compute abs path from config.
    private String storagePath;
    @Column(columnDefinition = "text")
    private String parsedText;

    public Resume(){}

    public Resume(User user, String originalFilename, String storedFilename,
                  String contentType, Long sizeBytes,
                  LocalDateTime uploadedAt, String storagePath) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
        this.storagePath = storagePath;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }

    @Override
    public String toString() {
        return "Resume{" +
                "id=" + id +
                ", user=" + user +
                ", originalFilename='" + originalFilename + '\'' +
                ", storedFilename='" + storedFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", uploadedAt=" + uploadedAt +
                ", storagePath='" + storagePath + '\'' +
                '}';
    }
}

