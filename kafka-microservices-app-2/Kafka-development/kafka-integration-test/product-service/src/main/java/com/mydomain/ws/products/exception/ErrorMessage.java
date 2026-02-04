package com.mydomain.ws.products.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
public class ErrorMessage {

    private Date timestamp = new Date();
    private String message;
    private String details;

    public ErrorMessage(String message) {
        this.message = message;
    }
}
