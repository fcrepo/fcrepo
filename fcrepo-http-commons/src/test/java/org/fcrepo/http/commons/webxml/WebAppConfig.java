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
package org.fcrepo.http.commons.webxml;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.fcrepo.http.commons.webxml.bind.ContextParam;
import org.fcrepo.http.commons.webxml.bind.Displayable;
import org.fcrepo.http.commons.webxml.bind.Filter;
import org.fcrepo.http.commons.webxml.bind.FilterMapping;
import org.fcrepo.http.commons.webxml.bind.Listener;
import org.fcrepo.http.commons.webxml.bind.Servlet;
import org.fcrepo.http.commons.webxml.bind.ServletMapping;

/**
 * <p>WebAppConfig class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "web-app")
public class WebAppConfig extends Displayable {

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "context-param")})
    List<ContextParam> contextParams;

    @XmlElements(
            value = {@XmlElement(
                    namespace = "http://java.sun.com/xml/ns/javaee",
                    name = "listener")})
    List<Listener> listeners;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee", name = "servlet")})
    List<Servlet> servlets;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee", name = "filter")})
    List<Filter> filters;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "servlet-mapping")})
    List<ServletMapping> servletMappings;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-mapping")})
    List<FilterMapping> filterMappings;

    public Collection<ServletMapping> servletMappings(final String servletName) {
        return servletMappings.stream().filter(new SMapFilter(servletName)).collect(Collectors.toList());
    }

    public Collection<FilterMapping> filterMappings(final String filterName) {
        return filterMappings.stream().filter(new FMapFilter(filterName)).collect(Collectors.toList());
    }

    private static List<ContextParam> NO_CP = unmodifiableList(new ArrayList<ContextParam>(0));

    public Collection<ContextParam> contextParams() {
        return (contextParams != null) ? contextParams : NO_CP;
    }

    private static List<Servlet> NO_S = unmodifiableList(new ArrayList<Servlet>(0));

    public Collection<Servlet> servlets() {
        return (servlets != null) ? servlets : NO_S;
    }

    private static List<Filter> NO_F = unmodifiableList(new ArrayList<Filter>(0));

    public Collection<Filter> filters() {
        return (filters != null) ? filters : NO_F;
    }

    private static List<Listener> NO_L =
        unmodifiableList(new ArrayList<Listener>(0));

    public Collection<Listener> listeners() {
        return (listeners != null) ? listeners : NO_L;
    }

    private static class SMapFilter implements Predicate<ServletMapping> {

        String servletName;

        SMapFilter(final String sName) {
            servletName = sName;
        }

        @Override
        public boolean test(final ServletMapping input) {
            return (servletName == null) ? input.servletName() == null
                    : servletName.equals(input.servletName());
        }

    }

    private static class FMapFilter implements Predicate<FilterMapping> {

        String filterName;

        FMapFilter(final String sName) {
            filterName = sName;
        }

        @Override
        public boolean test(final FilterMapping input) {
            return (filterName == null) ? input.filterName() == null
                    : filterName.equals(input.filterName());
        }

    }
}
