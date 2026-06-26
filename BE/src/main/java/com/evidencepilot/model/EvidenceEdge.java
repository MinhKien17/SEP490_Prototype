package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evidence_edges")
@Getter
@Setter
public class EvidenceEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "claim_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Claim claim;

    @ManyToOne
    @JoinColumn(name = "document_chunk_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private DocumentChunk documentChunk;

    private String verdict;

    @Column(name = "confidence_score", nullable = false)
    private Float confidenceScore;

    private String explanation;

    @Column(name = "missing_evidence", columnDefinition = "TEXT")
    private String missingEvidence;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EvidenceEdge that = (EvidenceEdge) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
