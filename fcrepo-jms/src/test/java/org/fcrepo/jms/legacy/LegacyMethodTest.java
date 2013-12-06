/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.jms.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.model.Text;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({EntryFactory.class})
public class LegacyMethodTest {

    private LegacyMethod testObj;

    private Entry mockDelegate;

    private Event mockEvent;

    private Category mockPidCategory;

    private Category mockDsidCategory;

    private static final String SOURCE_DSID = "sourceDsid";

    private static final String SOURCE_PID = "sourcePid";

    @Before
    public void setUp() throws RepositoryException {
        // set up the supporting mocks for the constructor
        mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn(javax.jcr.observation.Event.NODE_ADDED);
        when(mockEvent.getPath()).thenReturn("/" + SOURCE_PID + "/" + SOURCE_DSID);
        when(mockEvent.getInfo()).thenReturn(
                Collections.singletonMap(
                        FedoraEvent.NODE_TYPE_KEY,
                        FedoraJcrTypes.FEDORA_DATASTREAM));
        mockDelegate = mock(Entry.class);
        Text mockText = mock(Text.class);
        when(mockDelegate.setTitle(anyString())).thenReturn(mockText);
        when(mockDelegate.getTitle()).thenReturn("ingest");
        // make sure the delegate Entry can be instrumented for tests
        PowerMockito.mockStatic(EntryFactory.class);
        when(EntryFactory.newEntry()).thenReturn(mockDelegate);
        mockPidCategory = mock(Category.class);
        when(mockPidCategory.getLabel()).thenReturn(
                LegacyMethod.PID_CATEGORY_LABEL);
        when(mockPidCategory.getTerm()).thenReturn(SOURCE_PID);
        mockDsidCategory = mock(Category.class);
        when(mockDsidCategory.getLabel()).thenReturn(
                LegacyMethod.DSID_CATEGORY_LABEL);
        when(mockDsidCategory.getTerm()).thenReturn(SOURCE_DSID);
        List<Category> categories =
                Arrays.asList(new Category[] {mockPidCategory, mockDsidCategory});
        when(mockDelegate.getCategories(LegacyMethod.FEDORA_ID_SCHEME))
                .thenReturn(categories);
        // construct the test object
        testObj = new LegacyMethod(mockEvent);
    }

    @Test
    public void testPidAccessors() {
        String newPid = "newPid";
        assertEquals(SOURCE_PID, testObj.getPid());
        testObj.setPid(newPid);
        verify(mockPidCategory).setTerm(SOURCE_PID);
        verify(mockPidCategory).setTerm(newPid);
    }

    @Test
    public void testDsidAccessors() {
        String newDsid = "newDsid";
        assertEquals(SOURCE_DSID, testObj.getDsId());
        testObj.setDsId(newDsid);
        verify(mockDsidCategory).setTerm(SOURCE_DSID);
        verify(mockDsidCategory).setTerm(newDsid);
    }

    @Test
    public void textGetEntry() {
        Entry mockEntry = mock(Entry.class);
        LegacyMethod to = new LegacyMethod(mockEntry);
        assertEquals(mockEntry, to.getEntry());
    }

    @Test
    public void testMethodNameAccessors() {
        when(mockDelegate.getTitle()).thenReturn("foo");
        testObj.getMethodName();
        // called once in the constructor, once in the accessor
        verify(mockDelegate, times(2)).getTitle();
        testObj.setMethodName("foo");
        Text mockText = mock(Text.class);
        when(mockDelegate.setTitle(anyString())).thenReturn(mockText);
        String newTitle = "bar";
        testObj.setMethodName(newTitle);
        verify(mockDelegate.setTitle(newTitle));
    }

    @Test
    public void testModifiedAccesors() {
        testObj.getModified();
        verify(mockDelegate).getUpdated();
        testObj.setModified(new Date());
        // called once in the constructor, once in the accessor
        verify(mockDelegate, times(2)).setUpdated(any(Date.class));
    }

    @Test
    public void testUserIdAccessors() {
        Person mockPerson = mock(Person.class);
        when(mockDelegate.getAuthor()).thenReturn(mockPerson);
        testObj.getUserID();
        testObj.setUserId("foo");
        verify(mockDelegate).addAuthor(eq("foo"), anyString(), anyString());
        testObj.setUserId(null);
        // called once in the constructor, once in the accessor
        verify(mockDelegate, times(2)).addAuthor(eq("unknown"), anyString(),
                anyString());
    }

    @Test
    public void testSetContent() {
        testObj.setContent("foo");
        verify(mockDelegate).setContent("foo");
    }

    @Test
    public void testWriteTo() throws IOException {
        Writer mockWriter = mock(Writer.class);
        testObj.writeTo(mockWriter);
        verify(mockDelegate).writeTo(mockWriter);
    }

    @Test
    public void testGetBaseURL() {
        String url = testObj.getBaseURL();
        assertNotNull(url);
        assertEquals("http://localhost:8080/rest", url);

        final String host = "my-host";
        System.setProperty("fcrepo.host", host);

        url = testObj.getBaseURL();
        assertNotNull(url);
        assertEquals("http://" + host + ":8080/rest", url);

        final String port = "443";
        System.setProperty("fcrepo.port", port);

        url = testObj.getBaseURL();
        assertNotNull(url);
        assertEquals("https://" + host + ":" + port + "/rest", url);

        final String ctxt = "sleep";
        System.setProperty("fcrepo.ctxt", ctxt);

        url = testObj.getBaseURL();
        assertNotNull(url);
        assertEquals("https://" + host + ":" + port + "/" + ctxt, url);

        System.clearProperty("fcrepo.host");
        System.clearProperty("fcrepo.port");
        System.clearProperty("fcrepo.ctxt");
    }

}
