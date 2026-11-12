package com.example.lms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificates", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter
@Setter
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;
    @Column(nullable = false)
    private LocalDate issuedAt = LocalDate.now();
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uniqueCode = UUID.randomUUID().toString();
}
