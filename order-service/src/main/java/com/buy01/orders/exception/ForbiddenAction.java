package com.buy01.orders.exception;

public class ForbiddenAction extends RuntimeException {
    public ForbiddenAction(String message) {
        super(message);
    }
}
