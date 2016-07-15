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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>Listener class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "listener")
public class Listener extends Displayable {

    public Listener() {
    }

    public Listener(final String displayName, final String className) {
        this.displayName = displayName;
        this.className = className;
    }

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "listener-class")
    String className;

    public String className() {
        return this.className;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof Listener) {
            final Listener that = (Listener) object;
            final boolean className =
                (this.className == null) ? that.className == null
                    : this.className.equals(that.className);
            final boolean displayName =
                (this.displayName == null) ? that.displayName == null
                    : this.displayName.equals(that.displayName);
            return className && displayName;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + 2 * displayName.hashCode();
    }

}
