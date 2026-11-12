package com.example.lms.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("Этот email уже используется");
    }
}
