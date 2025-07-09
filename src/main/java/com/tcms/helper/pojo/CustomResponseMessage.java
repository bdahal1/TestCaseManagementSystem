package com.tcms.helper.pojo;

import java.util.Date;

public record CustomResponseMessage(Date timestamp, String message, String description) {
}