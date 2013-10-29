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

package org.fcrepo.kernel.utils.iterators;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * A stream of RDF triples along with some useful context.
 *
 * @author ajs6f
 * @date Oct 9, 2013
 */
public class RdfStream extends ForwardingIterator<Triple> implements
        Iterable<Triple> {

    private Map<String, String> namespaces = new HashMap<String, String>();

    protected Iterator<Triple> triples;

    private final static Triple[] NONE = new Triple[] {};

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples
     */
    public <Tr extends Triple, T extends Iterator<Tr>> RdfStream(final T triples) {
        super();
        this.triples = Iterators.transform(triples, cast());
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples
     */
    public <Tr extends Triple, T extends Iterable<Tr>> RdfStream(final T triples) {
        this(triples.iterator());
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples
     */
    public <Tr extends Triple, T extends Collection<Tr>> RdfStream(
            final T triples) {
        this(triples.iterator());
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples
     */
    public <T extends Triple> RdfStream(final T... triples) {
        this(Iterators.forArray(triples));
    }

    /**
     * Constructor that begins the stream with proffered statements.
     *
     * @param statements
     */
    public <T extends Statement> RdfStream(final T... statements) {
        this(Iterators.transform(Iterators.forArray(statements),
                statement2triple));
    }

    /**
     * Constructor that begins the stream with proffered triple.
     *
     * @param triple
     */
    public <T extends Triple> RdfStream(final T triple) {
        this(Iterators.forArray(new Triple[] {triple}));
    }

    /**
     * Constructor that begins the stream without any triples.
     */
    public RdfStream() {
        this(NONE);
    }

    /**
     * Returns the proffered {@link Triple}s with the context of this RdfStream.
     *
     * @param stream
     * @return
     */
    public <Tr extends Triple, T extends Iterator<Tr>> RdfStream withThisContext(final T stream) {
        return new RdfStream(stream).addNamespaces(namespaces());
    }

    /**
     * Returns the proffered {@link Triple}s with the context of this RdfStream.
     *
     * @param stream
     * @return
     */
    public <Tr extends Triple, T extends Iterable<Tr>> RdfStream withThisContext(final T stream) {
        return new RdfStream(stream).addNamespaces(namespaces());
    }

    /**
     * @param newTriples Triples to add.
     * @return This object for continued use.
     */
    public RdfStream concat(final Iterator<? extends Triple> newTriples) {
        triples = Iterators.concat(newTriples, triples);
        return this;
    }

    /**
     * @param newTriple Triples to add.
     * @return This object for continued use.
     */
    public <T extends Triple> RdfStream concat(final T newTriple) {
        triples = Iterators.concat(singletonIterator(newTriple), triples);
        return this;
    }

    /**
     * @param newTriples Triples to add.
     * @return This object for continued use.
     */
    public <T extends Triple> RdfStream concat(final T... newTriples) {
        triples = Iterators.concat(Iterators.forArray(newTriples), triples);
        return this;
    }

    /**
     * @param newTriples Triples to add.
     * @return This object for continued use.
     */
    public RdfStream concat(final Collection<? extends Triple> newTriples) {
        triples = Iterators.concat(newTriples.iterator(), triples);
        return this;
    }

    /**
     * As {@link Iterables#limit(Iterable, int)} while maintaining context.
     *
     * @param limit
     * @return
     */
    public RdfStream limit(final Integer limit) {
        if (limit < 0) {
            return this;
        }
        return withThisContext(Iterables.limit(this, limit));
    }

    /**
     * As {@link Iterables#skip(Iterable, int)} while maintaining context.
     *
     * @param skipNum
     * @return
     */
    public RdfStream skip(final Integer skipNum) {
        if (skipNum < 0) {
            return this;
        }
        return withThisContext(Iterables.skip(this, skipNum));
    }

    /**
     * As {@link Iterables#filter(Iterable, Predicate)} while maintaining context.
     *
     * @param predicate
     * @return
     */
    public RdfStream filter(final Predicate<? super Triple> predicate) {
        return withThisContext(Iterables.filter(this, predicate));
    }

    /**
     * As {@link Iterators#transform(Iterator, Function)}.
     *
     * @param f
     * @return
     */
    public <ToType> Iterator<ToType> transform(final Function<? super Triple, ToType> f) {
        return Iterators.transform(this, f);
    }

    /**
     * @param prefix
     * @param uri
     * @return This object for continued use.
     */
    public RdfStream addNamespace(final String prefix, final String uri) {
        namespaces.put(prefix, uri);
        return this;
    }

    /**
     * @param nses
     * @return This object for continued use.
     */
    public RdfStream addNamespaces(final Map<String, String> nses) {
        namespaces.putAll(nses);
        return this;
    }

    /**
     * WARNING!
     *
     * This method exhausts the RdfStream on which it is called!
     *
     * @return A {@link Model} containing the prefix mappings and triples in
     *         this stream of RDF
     */
    public Model asModel() {
        final Model model = createDefaultModel();
        model.setNsPrefixes(namespaces());
        for (final Triple t : this) {
            model.add(model.asStatement(t));
        }
        return model;
    }

    /**
     * @param model A {@link Model} containing the prefix mappings and triples to be put into
     *         this stream of RDF
     * @return
     */
    public static RdfStream fromModel(final Model model) {
        final Iterator<Triple> triples = Iterators.transform(model.listStatements(), statement2triple);
        return new RdfStream(triples).addNamespaces(model.getNsPrefixMap());
    }

    private static Function<Statement, Triple> statement2triple = new Function<Statement, Triple>() {

        @Override
        public Triple apply(final Statement s) {
            return s.asTriple();
        }

    };

    @Override
    protected Iterator<Triple> delegate() {
        return triples;
    }

    @Override
    public Iterator<Triple> iterator() {
        return this;
    }

    /**
     * @return Namespaces in scope for this stream.
     */
    public Map<String, String> namespaces() {
        return namespaces;
    }


    private static <T extends Triple> Function<T, Triple> cast() {
        return new Function<T, Triple>() {

            @Override
            public Triple apply(final T prototriple) {
                return prototriple;
            }

        };
    }

    /*
     * We ignore duplicated triples for equality.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof RdfStream) {
            final RdfStream rdfo = (RdfStream) o;
            final boolean triplesEqual =
                copyOf(rdfo.triples).equals(copyOf(this.triples));
            final boolean namespaceMappingsEqual =
                rdfo.namespaces().equals(this.namespaces());
            return triplesEqual && namespaceMappingsEqual;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return namespaces().hashCode() + 2 * triples.hashCode();
    }

}
