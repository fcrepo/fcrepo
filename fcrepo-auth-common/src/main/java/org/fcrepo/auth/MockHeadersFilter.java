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

package org.fcrepo.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.http.auth.BasicUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 */
public class MockHeadersFilter implements Filter {

    Logger logger = LoggerFactory.getLogger(MockHeadersFilter.class);

    Map<String, List<String>> mockHeaders =
            new HashMap<String, List<String>>();

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse res, final FilterChain chain)
        throws IOException, ServletException {
        logger.debug("in filter");
        if (req instanceof HttpServletRequest) {
            final HttpServletRequest hreq = (HttpServletRequest) req;
            final MockedHTTPServletRequest wreq =
                    new MockedHTTPServletRequest(hreq);
            chain.doFilter(wreq, res);
        } else {
            chain.doFilter(req, res);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(final FilterConfig config) throws ServletException {
        final Enumeration<String> params = config.getInitParameterNames();
        while (params.hasMoreElements()) {
            final String param = params.nextElement();
            final String val = config.getInitParameter(param);
            final List<String> headers = new ArrayList<String>();
            Collections.addAll(headers, val.split("\\|"));
            this.mockHeaders.put(param, headers);
        }
        logger.debug("config loaded with " + mockHeaders.size() +
                " headers mocked.");
    }

    private class MockedHTTPServletRequest extends
            HttpServletRequestWrapper {

        /**
         * @param request
         */
        public MockedHTTPServletRequest(final HttpServletRequest request) {
            super(request);
        }

        Principal userPrincipal = null;

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper
         * #getAuthType ()
         */
        @Override
        public String getAuthType() {
            return BASIC_AUTH;
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#
         * getRemoteUser()
         */
        @Override
        public String getRemoteUser() {
            return mockHeaders.get("REMOTE_USER").get(0);
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#
         * getUserPrincipal()
         */
        @Override
        public Principal getUserPrincipal() {
            if (userPrincipal == null) {
                userPrincipal =
                        new BasicUserPrincipal(mockHeaders.get(
                                "REMOTE_USER").get(0));
            }
            return userPrincipal;
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper
         * #getHeader (java.lang.String)
         */
        @Override
        public String getHeader(final String name) {
            if (mockHeaders.containsKey(name)) {
                return mockHeaders.get(name).get(0);
            }
            return super.getHeader(name);
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#isUserInRole(java.lang
         * .String)
         */
        @Override
        public boolean isUserInRole(final String role) {
            return ServletContainerAuthenticationProvider.FEDORA_USER
                    .equals(role);
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper#
         * getHeaderNames()
         */
        @Override
        public Enumeration<String> getHeaderNames() {
            final List<String> result = new ArrayList<String>();
            final Enumeration<String> orig = super.getHeaderNames();
            while (orig.hasMoreElements()) {
                result.add(orig.nextElement());
            }
            result.addAll(mockHeaders.keySet());
            final Vector<String> v = new Vector<String>(result);
            return v.elements();
        }

        /*
         * (non-Javadoc)
         * @see
         * javax.servlet.http.HttpServletRequestWrapper
         * #getHeaders (java.lang.String)
         */
        @Override
        public Enumeration<String> getHeaders(final String name) {
            if (mockHeaders.containsKey(name)) {
                return new Vector<String>(mockHeaders.get(name))
                        .elements();
            }
            return super.getHeaders(name);
        }
    }

}
