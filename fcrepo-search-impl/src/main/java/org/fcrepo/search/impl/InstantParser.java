/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * A utility class for parsing a variety of different date/time formats into an Instant.
 *
 * @author dbernstein
 */
public class InstantParser {

    private static final List<DateTimeFormatter> VALID_DATE_FORMATS = new ArrayList<>();

    static {
        VALID_DATE_FORMATS.add(ISO_DATE_TIME);
        VALID_DATE_FORMATS.add(ISO_OFFSET_DATE_TIME);
        VALID_DATE_FORMATS.add(RFC_1123_DATE_TIME);
        final var zoneId = ZoneOffset.UTC;
        VALID_DATE_FORMATS.add(new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd")
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0).toFormatter().withZone(zoneId));
        VALID_DATE_FORMATS.add(new DateTimeFormatterBuilder().appendPattern("yyyyMMdd")
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0).toFormatter().withZone(zoneId));
        VALID_DATE_FORMATS.add(new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss")
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0).toFormatter().withZone(zoneId));
        VALID_DATE_FORMATS.add(new DateTimeFormatterBuilder().appendPattern("yyyyMMdd HH:mm:ss")
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0).toFormatter().withZone(zoneId));
    }

    private InstantParser() { }

    /**
     * Parse a datestring into an instant.  If timezone or time information is missing, UTC is assumed.
     *
     * @param dateString The date string
     * @return an instant
     */
    public static Instant parse(final String dateString) {
        for (final DateTimeFormatter formatter : VALID_DATE_FORMATS) {
            try {
                final var temporalAccessor = formatter.parse(dateString);
                return Instant.from(temporalAccessor);
            } catch (final Exception e) {
                //ignore failures - if no date string is parsable, an error is thrown below
            }
        }
        throw new IllegalArgumentException("Invalid date format: \"" + dateString + "\"");
    }

}
