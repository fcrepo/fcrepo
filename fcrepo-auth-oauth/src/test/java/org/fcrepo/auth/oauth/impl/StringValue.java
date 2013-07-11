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
package org.fcrepo.auth.oauth.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;


public class StringValue implements Value {
    
    private final String value;
    
    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public String getString() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException,
            RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public boolean getBoolean() throws ValueFormatException,
            RepositoryException {
        throw new ValueFormatException("this is a testing String property");
    }

    @Override
    public int getType() {
        return PropertyType.STRING;
    }

}
