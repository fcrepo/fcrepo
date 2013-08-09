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

package org.fcrepo.kernel;

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


public class DummyURIResource implements Resource {

    final String uri;
    
    public DummyURIResource(String uri) {
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
    public <T extends RDFNode> T as(Class<T> view) {
        return null;
    }

    @Override
    public <T extends RDFNode> boolean canAs(Class<T> view) {
        return false;
    }

    @Override
    public Model getModel() {
        return null;
    }

    @Override
    public Object visitWith(RDFVisitor rv) {
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
    public Resource inModel(Model m) {
        return null;
    }

    @Override
    public boolean hasURI(String uri) {
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
    public Statement getRequiredProperty(Property p) {
        return null;
    }

    @Override
    public Statement getProperty(Property p) {
        return null;
    }

    @Override
    public StmtIterator listProperties(Property p) {
        return null;
    }

    @Override
    public StmtIterator listProperties() {
        return null;
    }

    @Override
    public Resource addLiteral(Property p, boolean o) {
        return null;
    }

    @Override
    public Resource addLiteral(Property p, long o) {
        return null;
    }

    @Override
    public Resource addLiteral(Property p, char o) {
        return null;
    }

    @Override
    public Resource addLiteral(Property value, double d) {
        return null;
    }

    @Override
    public Resource addLiteral(Property value, float d) {
        return null;
    }

    @Override
    public Resource addLiteral(Property p, Object o) {
        return null;
    }

    @Override
    public Resource addLiteral(Property p, Literal o) {
        return null;
    }

    @Override
    public Resource addProperty(Property p, String o) {
        return null;
    }

    @Override
    public Resource addProperty(Property p, String o, String l) {
        return null;
    }

    @Override
    public Resource addProperty(Property p, String lexicalForm,
            RDFDatatype datatype) {
        return null;
    }

    @Override
    public Resource addProperty(Property p, RDFNode o) {
        return null;
    }

    @Override
    public boolean hasProperty(Property p) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, boolean o) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, long o) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, char o) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, double o) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, float o) {
        return false;
    }

    @Override
    public boolean hasLiteral(Property p, Object o) {
        return false;
    }

    @Override
    public boolean hasProperty(Property p, String o) {
        return false;
    }

    @Override
    public boolean hasProperty(Property p, String o, String l) {
        return false;
    }

    @Override
    public boolean hasProperty(Property p, RDFNode o) {
        return false;
    }

    @Override
    public Resource removeProperties() {
        return null;
    }

    @Override
    public Resource removeAll(Property p) {
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
    public Resource getPropertyResourceValue(Property p) {
        return null;
    }

}
