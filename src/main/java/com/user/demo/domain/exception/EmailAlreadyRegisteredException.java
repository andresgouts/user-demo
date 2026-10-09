package com.user.demo.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("El correo ya registrado");
    }

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
