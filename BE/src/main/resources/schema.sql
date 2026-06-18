-- ============================================================
--  Evidence Pilot – MySQL 8.0 Schema  (v1.0)
--  Source of truth: matches JPA entity definitions exactly.
--
--  Usage:
--    • Docker: automatically executed by MySQL on the FIRST
--      container start via /docker-entrypoint-initdb.d/ mount.
--      Subsequent starts skip this file (volume already seeded).
--    • Manual: mysql -u root -p < schema.sql
-- ============================================================

-- Ensure we are targeting the correct database created by
-- the MYSQL_DATABASE env var.  The CREATE DATABASE below is
-- a no-op when the container already created it.
CREATE DATABASE IF NOT EXISTS `evidence_pilot`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `evidence_pilot`;

-- ── 1. users ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `users` (
    `id`            INT          NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt hash – never plain-text',
    `role`          ENUM('STUDENT','INSTRUCTOR','ADMIN') NOT NULL,
    `created_at`    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 2. projects ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `projects` (
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `student_id`  INT          NOT NULL,
    `title`       VARCHAR(255) NULL,
    `description` TEXT         NULL,
    `created_at`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_projects_student`
        FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 3. datasets ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `datasets` (
    `id`            INT          NOT NULL AUTO_INCREMENT,
    `instructor_id` INT          NOT NULL,
    `title`         VARCHAR(255) NULL,
    `created_at`    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_datasets_instructor`
        FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 4. claims ─────────────────────────────────────────────────
--  ai_confidence_score: DECIMAL(5,4) → stores 0.0000 – 1.0000
CREATE TABLE IF NOT EXISTS `claims` (
    `id`                  INT          NOT NULL AUTO_INCREMENT,
    `project_id`          INT          NOT NULL,
    `content`             TEXT         NOT NULL,
    `ai_confidence_score` DECIMAL(5,4) NULL COMMENT '0.0000–1.0000, populated by AI pipeline',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_claims_project`
        FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 5. sources ────────────────────────────────────────────────
--  project_id and dataset_id are both nullable (one may be absent).
CREATE TABLE IF NOT EXISTS `sources` (
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `file_url`    VARCHAR(500) NOT NULL COMMENT 'Absolute path inside the container',
    `project_id`  INT          NULL,
    `dataset_id`  INT          NULL,
    `uploaded_by` INT          NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_sources_project`
        FOREIGN KEY (`project_id`)  REFERENCES `projects` (`id`)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT `fk_sources_dataset`
        FOREIGN KEY (`dataset_id`)  REFERENCES `datasets` (`id`)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT `fk_sources_uploader`
        FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 6. papers ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `papers` (
    `id`           INT          NOT NULL AUTO_INCREMENT,
    `project_id`   INT          NOT NULL,
    `file_url`     VARCHAR(500) NOT NULL COMMENT 'Absolute path inside the container',
    `submitted_at` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_papers_project`
        FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 7. graphs ─────────────────────────────────────────────────
--  One-to-one with claims.  graph_data stores the full AI
--  ClaimAnalysisResponse as a native JSON document.
CREATE TABLE IF NOT EXISTS `graphs` (
    `id`         INT  NOT NULL AUTO_INCREMENT,
    `claim_id`   INT  NOT NULL,
    `graph_data` JSON NOT NULL COMMENT 'AI ClaimAnalysisResponse payload',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_graphs_claim` (`claim_id`),
    CONSTRAINT `fk_graphs_claim`
        FOREIGN KEY (`claim_id`) REFERENCES `claims` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 8. feedback_requests ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS `feedback_requests` (
    `id`            INT      NOT NULL AUTO_INCREMENT,
    `project_id`    INT      NOT NULL,
    `student_id`    INT      NOT NULL,
    `instructor_id` INT      NOT NULL,
    `status`        ENUM('PENDING','REVIEWED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `requested_at`  DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_feedback_req_project`
        FOREIGN KEY (`project_id`)    REFERENCES `projects` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `fk_feedback_req_student`
        FOREIGN KEY (`student_id`)    REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT `fk_feedback_req_instructor`
        FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ── 9. instructor_feedbacks ───────────────────────────────────
--  One-to-one with feedback_requests (uq_if_request enforces it).
CREATE TABLE IF NOT EXISTS `instructor_feedbacks` (
    `id`            INT      NOT NULL AUTO_INCREMENT,
    `request_id`    INT      NOT NULL,
    `instructor_id` INT      NOT NULL,
    `content`       TEXT     NOT NULL,
    `created_at`    DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_if_request` (`request_id`),
    CONSTRAINT `fk_if_request`
        FOREIGN KEY (`request_id`)    REFERENCES `feedback_requests` (`id`)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `fk_if_instructor`
        FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
