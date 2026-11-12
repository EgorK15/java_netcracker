package com.example.lms.exception;

public class CourseNotFoundException extends NotFoundException {
    public CourseNotFoundException() {
        super("Курс не найден");
    }
}
