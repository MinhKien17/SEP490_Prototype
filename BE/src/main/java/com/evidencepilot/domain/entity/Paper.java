package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a paper (PDF/document) submitted for a project.
 * Maps to the {@code papers} table.
 */
@Entity
@Table(name = "papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The project this paper belongs to.
     * Foreign key: papers.project_id → projects.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
