package com.example.mssqll.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Stores the (runtime-editable) SpEL formula that decides whether
 * {@code ConnectionFee.clarificationDate} should be set during an update.
 * <p>
 * A single active row drives the logic. The formula is evaluated against a map
 * of pre-computed boolean/string "facts" (see {@code ClarificationRuleService}).
 * Editing this row via the admin UI changes the business logic without a redeploy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clarification_rule")
public class ClarificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The editable SpEL boolean expression (evaluated against the facts map). */
    @Nationalized
    @Column(name = "formula", length = 4000, nullable = false)
    private String formula;

    /** Optional human note describing what this rule does / why it was changed. */
    @Nationalized
    @Column(name = "description", length = 2000)
    private String description;

    /** Only the active rule is evaluated at runtime. */
    @Column(name = "active", nullable = false)
    private boolean active;

    /** Username (email) of whoever last saved the formula. */
    @Nationalized
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
