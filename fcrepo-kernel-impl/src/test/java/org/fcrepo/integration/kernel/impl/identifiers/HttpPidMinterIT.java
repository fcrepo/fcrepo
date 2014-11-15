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
package org.fcrepo.integration.kernel.impl.identifiers;

import org.fcrepo.kernel.impl.identifiers.HttpPidMinter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

/**
 * <p>HttpPidMinterIT class.</p>
 *
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-container.xml")
public class HttpPidMinterIT{

    private static String PREFIX = "http://localhost:";

    private int getPort() {
        return parseInt(System.getProperty("test.port", "8080"));
    }

    @Inject
    private ContainerWrapper containerWrapper;

    private void addHandler(final String data1, final String path) {
        containerWrapper.addHandler(data1, path);
    }

    @Test
    public void shouldMintPid() throws IOException {
        final String res = "/res1";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPid2() throws IOException {
        final String res = "/res2";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST", "", "", " ", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPid3() throws IOException {
        final String res = "/res3";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "WHATEVER", "", "", " ", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept1() throws IOException {
        new HttpPidMinter(null, "POST", "", "", ".*/", "");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept2() throws IOException {
        new HttpPidMinter(null, "POST", "", "", ".*/", " ");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept3() throws IOException {
        new HttpPidMinter("http://test", "POST", "", "", ".*/", "\\wrongxpath");
    }

    @Test
    public void shouldMintPidWithGET() throws IOException {
        final String res = "/getres";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "GET", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidWithPUT() throws IOException {
        final String res = "/putres";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "PUT", "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidWithNullMethod() throws IOException {
        final String res = "/res4";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, null, "", "", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidXml() throws IOException {
        final String res = "/xml1";
        final String server = PREFIX + getPort() + res;
        addHandler("<test><id>baz</id></test>", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST"
                , "", "", "", "/test/id");
        final String pid = minter.mintPid();
        assertEquals(pid, "baz");
    }

    @Test
    public void shouldRunWithNoAuth() throws IOException {
        final String res = "/res5";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST",
                "fedoraAdmin", "secret", ".*/", null);
        final String pid = minter.mintPid();
        assertEquals(pid, "abc");
    }

    @Test (expected = RuntimeException.class)
    public void shouldMintPidXmlInvalid() throws IOException {
        final String res = "/xml2";
        final String server = PREFIX + getPort() + res;
        addHandler("<test><id>baz</id></tet>", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST"
                , "", "", "", "/test/id");
        minter.mintPid();
    }

}

