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
