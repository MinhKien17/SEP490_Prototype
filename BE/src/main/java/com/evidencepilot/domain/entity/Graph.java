package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Represents a knowledge graph generated for a claim.
 * The {@code graph_data} column is stored as native JSON via Hibernate 6's
 * {@link JdbcTypeCode} with {@link SqlTypes#JSON}.
 *
 * <p>The relationship with {@link Claim} is one-to-one (DBML dash notation).</p>
 *
 * Maps to the {@code graphs} table.
 */
@Entity
@Table(name = "graphs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Graph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The claim this graph visualises.
     * DBML defines a one-to-one relationship (dash notation).
     * Foreign key: graphs.claim_id → claims.id
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    /**
     * Graph payload stored as a JSON document.
     * Hibernate 6 maps this via the native JSON JDBC type — no manual
     * serialization needed. The Java type {@code Map<String, Object>} gives
     * maximum flexibility; swap for a dedicated DTO class if the schema
     * becomes well-defined.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph_data", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> graphData;
}
