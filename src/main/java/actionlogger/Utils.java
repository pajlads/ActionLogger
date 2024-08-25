package actionlogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Utils {

    public static String getTimestamp() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
