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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * a Value implementation to distinguish repository URIs from generic URI values
 * @author barmintor
 *
 */
public class ResourceUriValue implements Value {

    private final Resource resource;

    /**
     * 
     * @param resource
     */
    public ResourceUriValue(final Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
        // TODO Auto-generated method stub
        return resource.getURI();
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * 
     * @return the wrapped URI
     */
    public Resource getURI() {
        return resource;
    }

    @Override
    public int getType() {
        return PropertyType.URI;
    }

}
