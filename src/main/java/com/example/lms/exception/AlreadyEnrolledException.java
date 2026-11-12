package com.example.lms.exception;

public class AlreadyEnrolledException extends RuntimeException {
    public AlreadyEnrolledException() {
        super("Вы уже записаны на этот курс");
    }
}
