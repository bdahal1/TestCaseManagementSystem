package com.tcms.helper.pojo;

import lombok.Getter;

import java.util.Date;

@Getter
public class CustomResponseMessage {
    private Date timestamp;
    private String message;
    private String description;

    public CustomResponseMessage(Date timestamp, String message, String description) {
        this.timestamp = timestamp;
        this.message = message;
        this.description = description;
    }
}