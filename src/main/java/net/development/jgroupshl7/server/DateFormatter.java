package net.development.jgroupshl7.server;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    public static String formatDate(ZonedDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMdd'T'HHmmssX");
        return dateTime.format(formatter);
    }

    public String hl7AckTimeFormat() {
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("Australia/Sydney"));
        String formattedDate = formatDate(dateTime);
        return formattedDate; // Output: 20240512T182427+10
    }
}



