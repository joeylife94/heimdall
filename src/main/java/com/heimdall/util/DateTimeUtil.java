package com.heimdall.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    
    public static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    public static LocalDateTime parseIso(String isoString) {
        return LocalDateTime.parse(isoString, ISO_FORMATTER);
    }
    
    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }
    
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }
}
