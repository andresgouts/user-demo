package com.user.demo.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("El correo ya se ha registrado previamente");
    }

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
