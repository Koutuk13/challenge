package com.dws.challenge.exception;

public class InvalidAccountIDException extends RuntimeException{

    public InvalidAccountIDException(String message) {
        super(message);
    }
}
