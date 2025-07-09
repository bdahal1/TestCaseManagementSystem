package com.tcms.helper.pojo;

import java.util.Date;

public record ErrorMessageWithStatusCode(int statusCode, Date timestamp, String message, String description) {
}