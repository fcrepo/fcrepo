/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
