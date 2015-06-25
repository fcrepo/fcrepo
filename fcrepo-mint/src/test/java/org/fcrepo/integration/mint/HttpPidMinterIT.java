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
package org.fcrepo.integration.mint;

import org.fcrepo.mint.HttpPidMinter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

/**
 * <p>HttpPidMinterIT class.</p>
 *
 * @author osmandin
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-container.xml")
public class HttpPidMinterIT{

    private static String PREFIX = "http://localhost:";

    private static int getPort() {
        return parseInt(System.getProperty("fcrepo.dynamic.test.port", "8080"));
    }

    @Inject
    private ContainerWrapper containerWrapper;

    private void addHandler(final String data1, final String path) {
        containerWrapper.addHandler(data1, path);
    }

    @Test
    public void shouldMintPid() {
        final String res = "/res1";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST", "", "", ".*/", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPid2() {
        final String res = "/res2";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST", "", "", " ", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPid3() {
        final String res = "/res3";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "WHATEVER", "", "", " ", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept1() {
        new HttpPidMinter(null, "POST", "", "", ".*/", "");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept2() {
        new HttpPidMinter(null, "POST", "", "", ".*/", " ");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotAccept3() {
        new HttpPidMinter("http://test", "POST", "", "", ".*/", "\\wrongxpath");
    }

    @Test
    public void shouldMintPidWithGET() {
        final String res = "/getres";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "GET", "", "", ".*/", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidWithPUT() {
        final String res = "/putres";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "PUT", "", "", ".*/", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidWithNullMethod() {
        final String res = "/res4";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, null, "", "", ".*/", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test
    public void shouldMintPidXml() {
        final String res = "/xml1";
        final String server = PREFIX + getPort() + res;
        addHandler("<test><id>baz</id></test>", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST"
                , "", "", "", "/test/id");
        final String pid = minter.get();
        assertEquals(pid, "baz");
    }

    @Test
    public void shouldRunWithNoAuth() {
        final String res = "/res5";
        final String server = PREFIX + getPort() + res;
        addHandler("abc", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST",
                "fedoraAdmin", "secret", ".*/", null);
        final String pid = minter.get();
        assertEquals(pid, "abc");
    }

    @Test (expected = RuntimeException.class)
    public void shouldMintPidXmlInvalid() {
        final String res = "/xml2";
        final String server = PREFIX + getPort() + res;
        addHandler("<test><id>baz</id></tet>", res);
        final HttpPidMinter minter = new HttpPidMinter(server, "POST"
                , "", "", "", "/test/id");
        minter.get();
    }

}

