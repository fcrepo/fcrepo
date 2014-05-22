/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.rdf.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.GraphProperties;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.JcrPropertyStatementListener;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * @author cbeer
 * @author barmintor
 * @since 2013
 */
public class JcrGraphProperties implements GraphProperties {

    private static final String MODEL_NAME = "tree";

    @Override
    public String getPropertyModelName() {
        return MODEL_NAME;
    }

    @Override
    public Dataset getProperties(final Node node, final IdentifierTranslator subjects,
            final int offset, final int limit)
        throws RepositoryException {
        final JcrRdfTools jcrRdfTools = JcrRdfTools.withContext(subjects, node.getSession());
        final Model model =
            jcrRdfTools.getJcrTriples(node).concat(
                    jcrRdfTools.getTreeTriples(node)).limit(limit).skip(offset)
                    .asModel();
        final Model problemModel = JcrRdfTools.getProblemsModel();

        final JcrPropertyStatementListener listener =
            JcrPropertyStatementListener.getListener(
                    subjects, node.getSession(), problemModel);

        model.register(listener);

        final Dataset dataset = DatasetFactory.create(model);
        dataset.addNamedModel(MODEL_NAME, model);

        final Resource subject = subjects.getSubject(node.getPath());
        final String uri = subject.getURI();
        final com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        context.set(URI_SYMBOL,uri);
        context.set(INLINED_RESOURCES_MODEL,MODEL_NAME);

        dataset.addNamedModel(GraphProperties.PROBLEMS_MODEL_NAME,
                problemModel);

        return dataset;
    }

    @Override
    public Dataset getProperties(final Node node, final IdentifierTranslator subjects)
        throws RepositoryException {
        return getProperties(node, subjects, 0, -1);
    }

}
