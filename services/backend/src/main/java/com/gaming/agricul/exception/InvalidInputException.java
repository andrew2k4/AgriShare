package com.gaming.agricul.exception;

/** The request itself is malformed or breaks an input rule (mapped to 400 Bad Request). */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
