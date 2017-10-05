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

package org.fcrepo.kernel.api.utils;

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

/**
 * @author lsitu
 * @author Daniel Bernstein
 */
public class MessageExternalBodyContentType {

    public final static String MEDIA_TYPE = "message/external-body";

    private final static String MIME_TYPE_FIELD = "mime-type";

    /**
     * Utility method to parse the external body content in format: message/external-body; access-type=URL;
     * url="http://www.example.com/file"
     * 
     * @param mimeType the MimeType value for external resource
     * @return MessageExternalBodyContentType value
     * @throws UnsupportedAccessTypeException if mimeType param is not a valid message/external-body content type.
     */
    public static MessageExternalBodyContentType parse(final String mimeType) throws UnsupportedAccessTypeException {
        final Map<String, String> map = new HashMap<String, String>();
        Splitter.on(';').omitEmptyStrings().trimResults()
                .withKeyValueSeparator(Splitter.on('=').limit(2)).split(MIME_TYPE_FIELD + "=" + mimeType.trim())
                // use lower case for keys, unwrap the quoted values (double quotes at the beginning and the end)
                .forEach((k, v) -> map.put(k.toLowerCase(), v.replaceAll("^\"|\"$", "")));

        final String accessType = map.get("access-type").toLowerCase();
        final String resourceLocation = map.get(accessType);
        if (MEDIA_TYPE.equals(map.get(MIME_TYPE_FIELD)) &&
                "url".equals(accessType) &&
                !StringUtils.isBlank(resourceLocation)) {
            return new MessageExternalBodyContentType(accessType, resourceLocation);
        }

        throw new UnsupportedAccessTypeException(
                "The specified type is not a valid message/external body content type: value=" + mimeType);

    }

    private String accessType;

    private String resourceLocation;

    private MessageExternalBodyContentType(final String accessType, final String resourceLocation) {
        this.accessType = accessType;
        this.resourceLocation = resourceLocation;
    }

    /**
     * @return
     */
    public String getResourceLocation() {
        return this.resourceLocation;
    }

    /**
     * @return the accessType
     */
    public String getAccessType() {
        return accessType;
    }
}
