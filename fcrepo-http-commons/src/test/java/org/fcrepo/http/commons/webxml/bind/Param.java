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
package org.fcrepo.http.commons.webxml.bind;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * <p>Param class.</p>
 *
 * @author awoods
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Param extends Describable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "param-name")
    String name;

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "param-value")
    String value;

    public Param() {
    }

    public Param(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object object) {
        if (this.getClass().equals(object.getClass())) {
            final Param that = (Param) object;
            final boolean name =
                (this.name == null) ? that.name == null : this.name
                        .equals(that.name);
            final boolean value =
                (this.value == null) ? that.value == null : this.value
                        .equals(that.value);
            return name && value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + 2 * value.hashCode();

    }
}
