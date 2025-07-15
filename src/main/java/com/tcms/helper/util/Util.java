package com.tcms.helper.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class Util {
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Date parseDate(String date) {
        try {
            return new Date(DATE_FORMAT.parse(date).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Timestamp parseTimestamp(String timestamp) {
        try {
            return new Timestamp(DATE_TIME_FORMAT.parse(timestamp).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;

        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder capitalized = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return capitalized.toString().trim();
    }
}
