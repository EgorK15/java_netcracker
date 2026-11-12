package com.example.lms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instructors")
@Getter
@Setter
public class Instructor extends User {
    @Column(length = 2000)
    private String bio;
    @OneToMany(mappedBy = "author")
    private List<Course> courses = new ArrayList<>();
}
