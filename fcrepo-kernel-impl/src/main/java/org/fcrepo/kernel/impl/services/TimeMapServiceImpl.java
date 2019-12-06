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
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.services.TimeMapService;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link org.fcrepo.kernel.api.services.TimeMapService}
 *
 * @author dbernstein
 */
@Component
public class TimeMapServiceImpl extends AbstractService implements TimeMapService {

    @Override
    public boolean exists(final Transaction transaction, final String path) {
        return false;
    }

    @Override
    public TimeMap find(final Transaction transaction, final String path) {
        return null;
    }

    @Override
    public TimeMap findOrCreate(final Transaction transaction, final String path) {
        return null;
    }
}
