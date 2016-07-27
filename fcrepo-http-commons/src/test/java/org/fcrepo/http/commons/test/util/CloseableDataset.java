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
package org.fcrepo.http.commons.test.util;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetImpl;

/**
 * Adds the standard {@link AutoCloseable} semantic to Jena's {@link org.apache.jena.query.Dataset} for
 * convenient use with Java 7's <code>try-with-resources</code> syntax.
 *
 * @author ajs6f
 */
public class CloseableDataset extends DatasetImpl implements AutoCloseable {

    /**
     * Default constructor.
     *
     * @param model Model to wrap
     */
    public CloseableDataset(final Model model) {
        super(model);
    }
}
