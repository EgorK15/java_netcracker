package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.exception.*;
import com.example.lms.model.*;
import com.example.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;

    @Transactional
    public UserResponse createStudent(CreateStudentRequest request) {
        Student student = new Student();
        fillUser(student, request.firstName(), request.lastName(), request.email());
        return Mapper.user(studentRepository.save(student));
    }

    @Transactional
    public UserResponse createInstructor(CreateInstructorRequest request) {
        Instructor instructor = new Instructor();
        fillUser(instructor, request.firstName(), request.lastName(), request.email());
        instructor.setBio(request.bio());
        return Mapper.user(instructorRepository.save(instructor));
    }

    public UserResponse getStudent(Long id) {
        return Mapper.user(findStudent(id));
    }

    public UserResponse getInstructor(Long id) {
        return Mapper.user(findInstructor(id));
    }

    public Student findStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(StudentNotFoundException::new);
    }

    public Instructor findInstructor(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Инструктор не найден"));
    }

    private void fillUser(User user, String firstName, String lastName, String email) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }
        user.setFirstName(firstName.strip());
        user.setLastName(lastName.strip());
        user.setEmail(normalizedEmail);
    }
}
