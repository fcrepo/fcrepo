/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.observer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

/**
 * Created by ermadmix on 4/6/2015.
 * @author ermadmix
 */
public class FixityEventTest {

    //@Test(expected = java.lang.IllegalArgumentException.class)
    //public void testWrapNullFedoraEvent() {
    //    new org.fcrepo.kernel.observer.FixityEvent((org.fcrepo.kernel.observer.FixityEvent)null);
    //}

    org.fcrepo.kernel.observer.FixityEvent fixityEvent;
    @Before
    public void SetUp() {
        fixityEvent = new org.fcrepo.kernel.observer.FixityEvent();
        fixityEvent.setDate(1428335457057L);
        fixityEvent.setType(128);
        fixityEvent.setPath("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426");
        fixityEvent.setIdentifier("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426");
        fixityEvent.setBaseURL("http://localhost:8080/rest/");
        final HashMap<String,String> info = new HashMap<String,String>();
        info.put("http://www.loc.gov/premis/rdf/v1#hasMessageDigest",
                "urn:sha1:ca063d541865ab93d062f35800feb5011183abe6");
        info.put("http://www.loc.gov/premis/rdf/v1#hasSize","56114^^http://www.w3.org/2001/XMLSchema#long");
        info.put("http://fedora.info/definitions/v4/repository#status","SUCCESS");
        fixityEvent.setInfo(info);
        fixityEvent.setUserID("bypassAdmin");
        fixityEvent.setUserData("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
    }

    @Test
    public void testGetDate() {
        assertEquals(1428335457057L,fixityEvent.getDate());
    }

    @Test
    public void testGetType() {
        assertEquals(128, fixityEvent.getType());
    }

    @Test
    public void testGetPath() {
        assertEquals("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426",fixityEvent.getPath());
    }

    @Test
    public void testGetIdentifier() {
        assertEquals("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426",fixityEvent.getIdentifier());
    }

    @Test
    public void testGetBaseURL() {
        assertEquals("http://localhost:8080/rest/",fixityEvent.getBaseURL());
    }

    @Test
    public void testGetInfoLen() {
        assertEquals(3,fixityEvent.getInfo().size());
    }

    @Test
    public void testGetInfoContents() {
        assertTrue(fixityEvent.getInfo().containsKey("http://www.loc.gov/premis/rdf/v1#hasMessageDigest"));
        assertTrue(fixityEvent.getInfo().containsValue("urn:sha1:ca063d541865ab93d062f35800feb5011183abe6"));
    }

    @Test
    public void testGetUserID() {
        assertEquals("bypassAdmin", fixityEvent.getUserID());
    }

    @Test
    public void testGetUserData() {
        assertEquals("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
                fixityEvent.getUserData());
    }
}
