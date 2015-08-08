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
package org.fcrepo.kernel.api.utils.iterators;

import static com.google.common.base.Objects.equal;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.Objects.hash;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.jcr.Session;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * A stream of RDF triples along with some useful context.
 *
 * @author ajs6f
 * @since Oct 9, 2013
 */
public class RdfStream extends ForwardingIterator<Triple> {

    private final Map<String, String> namespaces = new HashMap<>();

    protected Iterator<Triple> triples;

    protected Session context;

    protected Node topic;

    private static final Triple[] NONE = new Triple[] {};

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples the triples
     * @param <Tr> extends {@link Triple}
     * @param <T> extends {@link Iterable}
     */
    public <Tr extends Triple, T extends Iterator<Tr>> RdfStream(final T triples) {
        super();
        this.triples = Iterators.transform(triples, cast()::apply);
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples the triples
     * @param <Tr> extends {@link Triple}
     * @param <T> extends {@link Iterable}
     */
    public <Tr extends Triple, T extends Iterable<Tr>> RdfStream(final T triples) {
        this(triples.iterator());
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples the triples
     * @param <Tr> extends {@link Triple}
     * @param <T> extends {@link Collection}
     */
    public <Tr extends Triple, T extends Collection<Tr>> RdfStream(
            final T triples) {
        this(triples.iterator());
    }

    /**
     * Constructor that begins the stream with proffered triples.
     *
     * @param triples the triples
     * @param <T> extends {@link Triple}
     */
    @SafeVarargs
    public <T extends Triple> RdfStream(final T... triples) {
        this(Iterators.forArray(triples));
    }

    /**
     * Constructor that begins the stream with proffered statements.
     *
     * @param statements the statements
     * @param <T> extends {@link Statement}
     */
    @SafeVarargs
    public <T extends Statement> RdfStream(final T... statements) {
        this(Iterators.transform(Iterators.forArray(statements),
                x -> x.asTriple()));
    }

    /**
     * Constructor that begins the stream with proffered triple.
     *
     * @param triple the triple
     * @param <T> extends {@link Triple}
     */
    public <T extends Triple> RdfStream(final T triple) {
        this(Iterators.forArray(new Triple[] { triple }));
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
     * @param stream the stream
     * @param <Tr> extends {@link Triple}
     * @param <T> extends {@link Iterator}
     * @return proffered Triples with the context of this RDFStream
     */
    public <Tr extends Triple, T extends Iterator<Tr>> RdfStream withThisContext(final T stream) {
        return new RdfStream(stream).namespaces(namespaces()).topic(topic());
    }

    /**
     * Returns the proffered {@link Triple}s with the context of this RdfStream.
     *
     * @param stream the stream
     * @param <Tr> extends {@link Triple}
     * @param <T> extends {@link Iterator}
     * @return proffered Triples with the context of this RDFStream
     */
    public <Tr extends Triple, T extends Iterable<Tr>> RdfStream withThisContext(final T stream) {
        return new RdfStream(stream).namespaces(namespaces()).topic(topic());
    }

    /**
     * @param newTriples Triples to add.
     * @return This object for continued use.
     */
    public RdfStream concat(final Iterator<? extends Triple> newTriples) {
        triples = Iterators.concat(triples, newTriples);
        return this;
    }

    /**
     * @param newTriple Triples to add.
     * @param <T> extends {@link Triple}
     * @return This object for continued use.
     */
    public <T extends Triple> RdfStream concat(final T newTriple) {
        triples = Iterators.concat(triples, singletonIterator(newTriple));
        return this;
    }

    /**
     * @param newTriples Triples to add.
     * @param <T> extends {@link Triple}
     * @return This object for continued use.
     */
    @SuppressWarnings("unchecked")
    public <T extends Triple> RdfStream concat(final T... newTriples) {
        triples = Iterators.concat(triples, Iterators.forArray(newTriples));
        return this;
    }

    /**
     * @param newTriples Triples to add.
     * @return This object for continued use.
     */
    public RdfStream concat(final Collection<? extends Triple> newTriples) {
        triples = Iterators.concat(triples, newTriples.iterator());
        return this;
    }

    /**
     * As {@link Iterators#limit(Iterator, int)} while maintaining context.
     *
     * @param limit the limit
     * @return RDFStream
     */
    public RdfStream limit(final Integer limit) {
        return (limit == -1) ? this : withThisContext(Iterators.limit(this, limit));
    }

    /**
     * As {@link Iterators#advance(Iterator, int)} while maintaining context.
     *
     * @param skipNum the skip number
     * @return RDFStream
     */
    public RdfStream skip(final Integer skipNum) {
        Iterators.advance(this, skipNum);
        return this;
    }

    /**
     * Filter the RDF triples while maintaining context.
     *
     * @param predicate the predicate
     * @return RdfStream
     */
    public RdfStream filter(final Predicate<? super Triple> predicate) {
        return withThisContext(Iterators.filter(this, predicate::test));
    }

    /**
     * Apply a Function to an Iterator.
     *
     * @param f the parameter f
     * @param <ToType> extends {@link Iterator}
     * @return Iterator
     */
    public <ToType> Iterator<ToType> transform(final Function<? super Triple, ToType> f) {
        return Iterators.transform(this, f::apply);
    }

    /**
     * RdfStream
     *
     * @param prefix the prefix
     * @param uri the uri
     * @return This object for continued use.
     */
    public RdfStream namespace(final String prefix, final String uri) {
        namespaces.put(prefix, uri);
        return this;
    }

    /**
     * @param nses the property of nses
     * @return This object for continued use.
     */
    public RdfStream namespaces(final Map<String, String> nses) {
        namespaces.putAll(nses);
        return this;
    }

    /**
     * @return The {@link Session} in context
     */
    public Session session() {
        return this.context;
    }

    /**
     * Sets the JCR context of this stream
     *
     * @param session The {@link Session} in context
     * @return the JCR context of this stream
     */
    public RdfStream session(final Session session) {
        this.context = session;
        return this;
    }

    /**
     * @return The {@link Node} topic in context
     */
    public Node topic() {
        return this.topic;
    }

    /**
     * Sets the topic of this stream
     *
     * @param topic The {@link Node} topic in context
     * @return the stream
     */
    public RdfStream topic(final Node topic) {
        this.topic = topic;
        return this;
    }

    /**
     * WARNING! This method exhausts the RdfStream on which it is called!
     *
     * @return A {@link Model} containing the prefix mappings and triples in this stream of RDF
     */
    public Model asModel() {
        final Model model = createDefaultModel();
        model.setNsPrefixes(namespaces());
        for (final Triple t : this.iterable()) {
            model.add(model.asStatement(t));
        }
        return model;
    }

    /**
     * @param model A {@link Model} containing the prefix mappings and triples to be put into this stream of RDF
     * @return RDFStream
     */
    public static RdfStream fromModel(final Model model) {
        final Iterator<Triple> triples = Iterators.transform(model.listStatements(), x -> x.asTriple());
        return new RdfStream(triples).namespaces(model.getNsPrefixMap());
    }

    @Override
    protected Iterator<Triple> delegate() {
        return triples;
    }

    /**
     * @return an anonymous {@literal Iterable<Triple>} for use with for-each etc.
     */
    public Iterable<Triple> iterable() {
        return new Iterable<Triple>() {

            @Override
            public Iterator<Triple> iterator() {
                return triples;
            }
        };
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

    protected static <From, To> Iterator<To> flatMap(final Iterator<From> i, final Function<From, Iterator<To>> f) {
        return Iterators.concat(Iterators.transform(i, f::apply));
    }

    /*
     * We ignore duplicated triples for equality. (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof RdfStream)) {
            return false;
        }
        final RdfStream rdfo = (RdfStream) o;

        final boolean triplesEqual =
                equal(copyOf(rdfo.triples), copyOf(this.triples));

        final boolean namespaceMappingsEqual =
                equal(rdfo.namespaces(), this.namespaces());

        final boolean topicEqual =
                equal(rdfo.topic(), this.topic());

        return triplesEqual && namespaceMappingsEqual && topicEqual;

    }

    @Override
    public int hashCode() {
        return hash(namespaces(), triples, topic());
    }

}
