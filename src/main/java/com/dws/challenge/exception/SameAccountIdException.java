package com.dws.challenge.exception;

public class SameAccountIdException extends RuntimeException{

    public SameAccountIdException(String message) {
        super(message);
    }
}
