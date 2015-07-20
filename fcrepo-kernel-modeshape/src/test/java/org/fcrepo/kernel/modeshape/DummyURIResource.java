/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * <p>DummyURIResource class.</p>
 *
 * @author awoods
 */
public class DummyURIResource implements Resource {

    final String uri;

    public DummyURIResource(final String uri) {
        this.uri = uri;
    }

    @Override
    public boolean isAnon() {
        return false;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isURIResource() {
        return true;
    }

    @Override
    public boolean isResource() {
        return false;
    }

    @Override
    public <T extends RDFNode> T as(final Class<T> view) {
        return null;
    }

    @Override
    public <T extends RDFNode> boolean canAs(final Class<T> view) {
        return false;
    }

    @Override
    public Model getModel() {
        return null;
    }

    @Override
    public Object visitWith(final RDFVisitor rv) {
        return null;
    }

    @Override
    public Resource asResource() {
        return null;
    }

    @Override
    public Literal asLiteral() {
        return null;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createURI(this.uri);
    }

    @Override
    public AnonId getId() {
        return null;
    }

    @Override
    public Resource inModel(final Model m) {
        return null;
    }

    @Override
    public boolean hasURI(final String uri) {
        return true;
    }

    @Override
    public String getURI() {
        return this.uri;
    }

    @Override
    public String getNameSpace() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public Statement getRequiredProperty(final Property p) {
        return null;
    }

    @Override
    public Statement getProperty(final Property p) {
        return null;
    }

    @Override
    public StmtIterator listProperties(final Property p) {
        return null;
    }

    @Override
    public StmtIterator listProperties() {
        return null;
    }

    @Override
    public Resource addLiteral(final Property p, final boolean o) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property p, final long o) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property p, final char o) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property value, final double d) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property value, final float d) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property p, final Object o) {
        return null;
    }

    @Override
    public Resource addLiteral(final Property p, final Literal o) {
        return null;
    }

    @Override
    public Resource addProperty(final Property p, final String o) {
        return null;
    }

    @Override
    public Resource addProperty(final Property p, final String o, final String l) {
        return null;
    }

    @Override
    public Resource addProperty(final Property p, final String lexicalForm,
                                final RDFDatatype datatype) {
        return null;
    }

    @Override
    public Resource addProperty(final Property p, final RDFNode o) {
        return null;
    }

    @Override
    public boolean hasProperty(final Property p) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final boolean o) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final long o) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final char o) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final double o) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final float o) {
        return false;
    }

    @Override
    public boolean hasLiteral(final Property p, final Object o) {
        return false;
    }

    @Override
    public boolean hasProperty(final Property p, final String o) {
        return false;
    }

    @Override
    public boolean hasProperty(final Property p, final String o, final String l) {
        return false;
    }

    @Override
    public boolean hasProperty(final Property p, final RDFNode o) {
        return false;
    }

    @Override
    public Resource removeProperties() {
        return null;
    }

    @Override
    public Resource removeAll(final Property p) {
        return null;
    }

    @Override
    public Resource begin() {
        return null;
    }

    @Override
    public Resource abort() {
        return null;
    }

    @Override
    public Resource commit() {
        return null;
    }

    @Override
    public Resource getPropertyResourceValue(final Property p) {
        return null;
    }

}
