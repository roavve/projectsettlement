package com.example.mssqll.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@Where(clause = "status != 'SOFT_DELETED'")
public class Extraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;//

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;//

    @Column(name = "purpose", columnDefinition = "nvarchar(max)", nullable = false)
    @Nationalized
    private String purpose;//

    @Lob
    @Column(name = "description", columnDefinition = "nvarchar(max)", nullable = false)
    @Nationalized
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "tax_id", length = 255, nullable = false)
    @Nationalized
    private String tax;

    @ManyToOne
    @JoinColumn(name = "extraction_task_id", nullable = false)
    private ExtractionTask extractionTask;//
}
