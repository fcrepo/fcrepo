/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api;

import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.fcrepo.kernel.impl.identifiers.HttpPidMinter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * <p>HttpPidMinterIT class.</p>
 *
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public class HttpPidMinterIT {

    private static final String PREFIX = "http://localhost:";

    @Inject
    ContainerWrapper containerWrapper;

    @Test
    public void shouldMintPid() throws IOException {
        containerWrapper.addHandler("baz");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(),
                "POST", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "baz");
    }

    @Test
    public void shouldMintPid2() throws IOException {
        containerWrapper.addHandler("baz2");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "POST", "", "", " ", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "baz2");
    }

    @Test
    public void shouldMintPid3() throws IOException {
        containerWrapper.addHandler("baz3");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(),
                "WHATEVER", "", "", " ", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "baz3");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept1() throws IOException {
        final HttpPidMinter minter = new HttpPidMinter(null, "POST", "", "", ".*/", "");
        assert(minter == null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept2() throws IOException {
        final HttpPidMinter minter = new HttpPidMinter(null, "POST", "", "", ".*/", " ");
        assert(minter == null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept3() throws IOException {
        final HttpPidMinter minter = new HttpPidMinter("http://test", "POST", "", "", ".*/", "\\wrongxpath");
        assert(minter == null);
    }

    @Test
    public void shouldMintPidWithGET() throws IOException {
        containerWrapper.addHandler("bazGet");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "GET", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "bazGet");
    }

    @Test
    public void shouldMintPidWithPUT() throws IOException {
        containerWrapper.addHandler("kaPUT?");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "PUT", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "kaPUT?");
    }

    @Test
    public void shouldMintPidWithNull() throws IOException {
        containerWrapper.addHandler("default");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), null, "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "default");
    }

    @Test
    public void shouldMintPidXml() throws IOException {
        containerWrapper.addHandler("<test><id>baz</id></test>");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "POST"
                , "", "", "", "/test/id");
        final String pid = minter.mintPid();
        assertEquals(pid, "baz");
    }

    @Test
    public void shouldRunWithNoAuth() throws IOException {
        containerWrapper.addHandler("baz");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "POST",
                "fedoraAdmin", "secret", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "baz");
    }

    @Test (expected = RuntimeException.class)
    public void shouldMintPidXmlInvalid() throws IOException {
        containerWrapper.addHandler("<test><id>baz</id></tet>");
        final HttpPidMinter minter = new HttpPidMinter(PREFIX + containerWrapper.getPort(), "POST"
                , "", "", "", "/test/id");
        final String pid = minter.mintPid();
        assertEquals(pid, "baz");
    }

}

