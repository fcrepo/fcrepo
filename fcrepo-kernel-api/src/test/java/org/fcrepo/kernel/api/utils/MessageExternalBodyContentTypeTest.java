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

import static org.junit.Assert.assertEquals;

import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;

import org.junit.Test;

/**
 * @author lsitu
 * @author dbernstein
 */
public class MessageExternalBodyContentTypeTest {

    private static String RESOURCE_URL = "http://www.example.com/file";

    private static String EXTERNAL_RESOURCE = "message/external-body; access-type=URL; URL=\"" + RESOURCE_URL + "\"";

    @Test
    public void testParseExternalBody() throws UnsupportedAccessTypeException {
        final MessageExternalBodyContentType contentType = MessageExternalBodyContentType.parse(EXTERNAL_RESOURCE);
        assertEquals("Access-type doesn't match.", "url", contentType.getAccessType());
        assertEquals("URL doesn't match.", RESOURCE_URL, contentType.getResourceLocation());
    }

    @Test(expected = UnsupportedAccessTypeException.class)
    public void testParseExternalBodyInvalidAccessType() throws UnsupportedAccessTypeException {
        MessageExternalBodyContentType.parse(
                "message/external-body; access-type=ftp; URL=\"" + RESOURCE_URL + "\"");
    }

    @Test(expected = UnsupportedAccessTypeException.class)
    public void testParseExternalBodyInvalidAccessTypeBlankURL() throws UnsupportedAccessTypeException {
        MessageExternalBodyContentType.parse(
                "message/external-body; access-type=ftp; URL=\"\"");
    }

    public void testParseExternalBodyAccessTypeCaseInsensitive() throws UnsupportedAccessTypeException {
        final MessageExternalBodyContentType contentType = MessageExternalBodyContentType.parse(
                "message/external-body; access-type=url; URL=\"" + RESOURCE_URL + "\"");
        assertEquals("Access-type doesn't match.", "url", contentType.getAccessType());
        assertEquals("URL doesn't match.", RESOURCE_URL, contentType.getResourceLocation());
    }

    public void testParseExternalBodyLocationKeyCaseInsensitive() throws UnsupportedAccessTypeException {
        final MessageExternalBodyContentType contentType = MessageExternalBodyContentType.parse(
                "message/external-body; access-type=URL; url=\"" + RESOURCE_URL + "\"");
        assertEquals("Access-type doesn't match.", "url", contentType.getAccessType());
        assertEquals("URL doesn't match.", RESOURCE_URL, contentType.getResourceLocation());
    }

    @Test(expected = UnsupportedAccessTypeException.class)
    public void testParseExternalBodyInvalidMimeType() throws UnsupportedAccessTypeException {
        MessageExternalBodyContentType.parse(
                "invalid/mimetype; access-type=url; URL=\"" + RESOURCE_URL + "\"");
    }

    @Test(expected = UnsupportedAccessTypeException.class)
    public void testParseExternalBodyMissingURL() throws UnsupportedAccessTypeException {
        MessageExternalBodyContentType.parse(
                "message/external-body; access-type=url;");
    }

}
