package actionlogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String getTimestamp() {
        var now = LocalDateTime.now();
        return now.format(DATE_TIME_FORMATTER);
    }
}
