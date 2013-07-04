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
package org.fcrepo.auth.oauth.integration.api.bind;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public class Filter extends Displayable {

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "init-param")})
    List<InitParam> initParams;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-name")})
    String filterName;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-class")})
    String filterClass;

    public String filterName() {
        return this.filterName;
    }

    public String filterClass() {
        return this.filterClass;
    }

    @SuppressWarnings("unchecked")
    public List<InitParam> initParams() {
        return (initParams != null) ? initParams : Collections.EMPTY_LIST;
    }

}
