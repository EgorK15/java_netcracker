package com.example.lms.service;

import com.example.lms.dto.CreateInstructorRequest;
import com.example.lms.dto.CreateStudentRequest;
import com.example.lms.exception.EmailAlreadyUsedException;
import com.example.lms.exception.StudentNotFoundException;
import com.example.lms.model.Student;
import com.example.lms.repository.InstructorRepository;
import com.example.lms.repository.StudentRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock StudentRepository students;
    @Mock InstructorRepository instructors;
    @InjectMocks UserService service;

    @Test
    void normalizesEmailBeforeSavingStudent() {
        when(students.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.createStudent(new CreateStudentRequest(" Иван ", "Иванов", "STUDENT@example.com"));

        assertThat(response.email()).isEqualTo("student@example.com");
        assertThat(response.firstName()).isEqualTo("Иван");
    }

    @Test
    void rejectsEmailTakenByAnyRole() {
        when(users.existsByEmail("teacher@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createInstructor(
                new CreateInstructorRequest("Анна", "Петрова", "Teacher@example.com", null)))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verifyNoInteractions(instructors);
    }

    @Test
    void reportsMissingStudent() {
        when(students.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStudent(1L)).isInstanceOf(StudentNotFoundException.class);
    }
}
