package com.example.mssqll.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;


@Entity
@Table(name = "business_units",
        indexes = {
                @Index(name = "idx_parent_id", columnList = "parent_id")
        })
@Getter
@Setter
@Data
public class BusinessUnit {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "unit_number")
    private Integer unitNumber;

    @Column(name = "name")
    @Nationalized
    private String name;

    @Column(name = "unit_type_key")
    private Integer unitTypeKey;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = true)
    private BusinessUnit parent;

    @Override
    public String toString() {
        return "BusinessUnit{" +
                "id=" + id +
                ", unitNumber=" + unitNumber +
                ", name='" + name + '\'' +
                ", unitTypeKey=" + unitTypeKey +
                '}';
    }
}
