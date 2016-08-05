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

import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.util.function.BiFunction;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.functions.Converter;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author barmintor
 *
 */
public class RowToResourceUri implements BiFunction<String, Value[], Value>, FedoraTypes {

    private final Converter<Resource, String> idTranslator;

    /**
     * 
     * @param idTranslator
     */
    public RowToResourceUri(final Converter<Resource, String> idTranslator) {
        this.idTranslator = idTranslator;
    }

    @Override
    public Value apply(final String path, final Value[] mixinTypes) {
        try {
            if (testForType(FEDORA_BINARY, mixinTypes)) {
                return new ResourceUriValue(idTranslator.toDomain(removeEnd(path, "/jcr:content")));
            } else if (testForType(FEDORA_NON_RDF_SOURCE_DESCRIPTION, mixinTypes)) {
                return new ResourceUriValue(idTranslator.toDomain(path + "/fcr:metadata"));
            } else if (testForType(FEDORA_TOMBSTONE, mixinTypes)) {
                return new ResourceUriValue(idTranslator.toDomain(path + "/fcr:tombstone"));
            } else {
                return new ResourceUriValue(idTranslator.toDomain(path));
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    static boolean testForType(final String type, final Value[] mixinTypes) throws RepositoryException {
        for (Value value: mixinTypes) {
            if (PropertyType.NAME == value.getType() && value.getString().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
