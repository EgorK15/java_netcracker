package com.example.lms.exception;

public class CourseNotPublishedException extends IllegalStateException {
    public CourseNotPublishedException() {
        super("Курс не опубликован");
    }
}
