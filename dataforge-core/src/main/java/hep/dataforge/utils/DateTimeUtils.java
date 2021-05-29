package hep.dataforge.utils;

import hep.dataforge.values.TimeValue;
import hep.dataforge.values.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Created by darksnake on 14-Oct-16.
 */
public class DateTimeUtils {
    private static DateTimeFormatter SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy_A");

    public static Instant now() {
        return LocalDateTime.now().toInstant(ZoneOffset.UTC);
    }

    /**
     * Build a unique file suffix based on current date-time
     *
     * @return
     */
    public static String fileSuffix() {
        return SUFFIX_FORMATTER.format(LocalDateTime.now());
    }

    public static Value nowValue() {
        return new TimeValue(now());
    }
}
