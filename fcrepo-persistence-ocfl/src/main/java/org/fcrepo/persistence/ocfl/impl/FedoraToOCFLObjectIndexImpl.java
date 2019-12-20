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
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An simple in-memory implementation of the {@link org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex}
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class FedoraToOCFLObjectIndexImpl implements FedoraToOCFLObjectIndex {
    private Map<String, FedoraOCFLMapping> fedoraOCFLMappingMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public FedoraOCFLMapping getMapping(final String fedoraResourceIdentifier)
            throws FedoraOCFLMappingNotFoundException {

        final FedoraOCFLMapping m = fedoraOCFLMappingMap.get(fedoraResourceIdentifier);
        if (m == null) {
            throw new FedoraOCFLMappingNotFoundException(fedoraResourceIdentifier);
        }

        return m;
    }

    @Override
    public FedoraOCFLMapping addMapping(final String fedoraResourceIdentifier, final String fedoraRootObjectResourceId, final String ocflObjectId) {
        FedoraOCFLMapping mapping = fedoraOCFLMappingMap.get(fedoraRootObjectResourceId);

        if (mapping == null) {
            mapping = new FedoraOCFLMapping(fedoraRootObjectResourceId, ocflObjectId);
            fedoraOCFLMappingMap.put(fedoraRootObjectResourceId, mapping);
        }

        if (!fedoraResourceIdentifier.equals(fedoraRootObjectResourceId)) {
            fedoraOCFLMappingMap.put(fedoraResourceIdentifier, mapping);
        }
        return mapping;
    }

}
