/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.session;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TX;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Constants related to transactions in HTTP requests
 *
 * @author bbpennel
 */
public class TransactionConstants {

    /**
     * Private constructor
     */
    private TransactionConstants() {
    }

    public static final String ATOMIC_ID_HEADER = "Atomic-ID";

    public static final String ATOMIC_EXPIRES_HEADER = "Atomic-Expires";

    public static final String TX_PREFIX = FCR_TX + "/";

    public static final String TX_NS = "http://fedora.info/definitions/v4/transaction#";

    public static final String TX_ENDPOINT_REL = TX_NS + "endpoint";

    public static final String TX_COMMIT_REL = TX_NS + "commitEndpoint";

    public static final DateTimeFormatter EXPIRES_RFC_1123_FORMATTER = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));
}
