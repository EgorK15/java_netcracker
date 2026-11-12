package com.example.lms.exception;

public class StudentNotFoundException extends NotFoundException {
    public StudentNotFoundException() {
        super("Студент не найден");
    }
}
