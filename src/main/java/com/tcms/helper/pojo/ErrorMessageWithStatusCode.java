package com.tcms.helper.pojo;

import lombok.Getter;

import java.util.Date;

@Getter
public class ErrorMessageWithStatusCode {
    private int statusCode;
    private Date timestamp;
    private String message;
    private String description;

    public ErrorMessageWithStatusCode(int statusCode, Date timestamp, String message, String description) {
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.message = message;
        this.description = description;
    }
}