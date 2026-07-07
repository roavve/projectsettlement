package com.example.mssqll.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * A user-defined "fact" the clarificationDate formula may reference. Each fact
 * attaches to a {@link ClarificationFactField} of the ConnectionFee and is derived
 * via a {@link ClarificationFactModification} (e.g. "projectID changed",
 * "orderStatus new value equals RETURNED"). Facts are created/edited from the admin
 * UI and computed at update time, then exposed to the formula as {@code #name}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clarification_fact")
public class ClarificationFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Formula variable name (referenced as {@code #name}). Must be a valid identifier and unique. */
    @Nationalized
    @Column(name = "name", length = 255, nullable = false, unique = true)
    private String name;

    /** Optional human note describing the fact. */
    @Nationalized
    @Column(name = "description", length = 2000)
    private String description;

    /** The ConnectionFee field this fact attaches to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_field", length = 64, nullable = false)
    private ClarificationFactField sourceField;

    /** How the fact is derived from the field's old/new value. */
    @Enumerated(EnumType.STRING)
    @Column(name = "modification_type", length = 64, nullable = false)
    private ClarificationFactModification modificationType;

    /** Result type (derived from the modification type; persisted for convenience). */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 16, nullable = false)
    private FactValueType valueType;

    /** First comparison value (e.g. the "equals" value or the transition "from" value). */
    @Nationalized
    @Column(name = "value_a", length = 1024)
    private String valueA;

    /** Second comparison value (the transition "to" value). */
    @Nationalized
    @Column(name = "value_b", length = 1024)
    private String valueB;

    /** Only active facts are computed at runtime and offered to the formula. */
    @Column(name = "active", nullable = false)
    private boolean active;

    @Nationalized
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
