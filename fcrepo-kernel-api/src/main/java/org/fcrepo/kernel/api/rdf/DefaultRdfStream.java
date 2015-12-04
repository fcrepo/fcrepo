/*
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
package org.fcrepo.kernel.api.rdf;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Stream.empty;
import static java.util.stream.StreamSupport.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Implementation of a context-bearing RDF stream
 *
 * @author acoburn
 * @since Dec 6, 2015
 */
public class DefaultRdfStream implements RdfStream {

    protected Stream<Triple> stream;
    protected final Node node;

    /**
     * Create an empty RdfStream
     */
    public DefaultRdfStream() {
        this(empty());
    }

    /**
     * Create an RdfStream
     * @param node the topic of the stream
     */
    public DefaultRdfStream(final Node node) {
        this(node, empty());
    }

    /**
     * Create an RdfStream
     *
     * @param stream a stream of triples
     */
    public DefaultRdfStream(final Stream<Triple> stream) {
        this.node = null;
        this.stream = stream;
    }

    /**
     * Create an RdfStream
     * @param node the topic of the stream
     * @param stream the incoming stream
     */
    public DefaultRdfStream(final Node node, final Stream<Triple> stream) {
        this.node = node;
        this.stream = stream;
    }

    /**
     * Create an RdfStream from an existing Model.
     * @param model An input Model
     * @return a new RdfStream object
     */
    public static RdfStream fromModel(final Model model) {
        return fromModel((Node)null, model);
    }

    /**
     * Create an RdfStream from an existing Model.
     * @param node The subject node
     * @param model An input Model
     * @return a new RdfStream object
     */
    public static RdfStream fromModel(final Node node, final Model model) {
        return new DefaultRdfStream(node,
                stream(spliteratorUnknownSize(model.listStatements(), IMMUTABLE), false).map(Statement::asTriple));
    }

    @Override
    public Node topic() {
        return node;
    }

    @Override
    public boolean allMatch(final Predicate<? super Triple> predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean anyMatch(final Predicate<? super Triple> predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public <R, A> R collect(final Collector<? super Triple, A, R> collector) {
        return stream.collect(collector);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<R, ? super Triple> accumulator,
            final BiConsumer<R,R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public RdfStream distinct() {
        return new DefaultRdfStream(topic(), stream.distinct());
    }

    @Override
    public RdfStream filter(final Predicate<? super Triple> predicate) {
        return new DefaultRdfStream(topic(), stream.filter(predicate));
    }

    @Override
    public Optional<Triple> findAny() {
        return stream.findAny();
    }

    @Override
    public Optional<Triple> findFirst() {
        return stream.findFirst();
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super Triple, ? extends Stream<? extends R>> mapper) {
        return stream.flatMap(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(final Function<? super Triple, ? extends DoubleStream> mapper) {
        return stream.flatMapToDouble(mapper);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super Triple, ? extends IntStream> mapper) {
        return stream.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super Triple, ? extends LongStream> mapper) {
        return stream.flatMapToLong(mapper);
    }

    @Override
    public void forEach(final Consumer<? super Triple> action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(final Consumer<? super Triple> action) {
        stream.forEachOrdered(action);
    }

    @Override
    public RdfStream limit(final long maxSize) {
        return new DefaultRdfStream(topic(), stream.limit(maxSize));
    }

    @Override
    public <R> Stream<R> map(final Function<? super Triple,? extends R> mapper) {
        return stream.map(mapper);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super Triple> mapper) {
        return stream.mapToDouble(mapper);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super Triple> mapper) {
        return stream.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super Triple> mapper) {
        return stream.mapToLong(mapper);
    }

    @Override
    public Optional<Triple> max(final Comparator<? super Triple> comparator) {
        return stream.max(comparator);
    }

    @Override
    public Optional<Triple> min(final Comparator<? super Triple> comparator) {
        return stream.min(comparator);
    }

    @Override
    public boolean noneMatch(final Predicate<? super Triple> predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public RdfStream peek(final Consumer<? super Triple> action) {
        return new DefaultRdfStream(topic(), stream.peek(action));
    }

    @Override
    public Optional<Triple> reduce(final BinaryOperator<Triple> accumulator) {
        return stream.reduce(accumulator);
    }

    @Override
    public Triple reduce(final Triple identity, final BinaryOperator<Triple> accumulator) {
        return stream.reduce(identity, accumulator);
    }

    @Override
    public <U> U reduce(final U identity, final BiFunction<U,? super Triple,U> accumulator,
            final BinaryOperator<U> combiner) {
        return stream.reduce(identity, accumulator, combiner);
    }

    @Override
    public RdfStream skip(final long n) {
        return new DefaultRdfStream(topic(), stream.skip(n));
    }

    @Override
    public RdfStream sorted() {
        return new DefaultRdfStream(topic(), stream.sorted());
    }

    @Override
    public RdfStream sorted(final Comparator<? super Triple> comparator) {
        return new DefaultRdfStream(topic(), stream.sorted(comparator));
    }

    @Override
    public Object[] toArray() {
        return stream.toArray();
    }

    @Override
    public <A> A[] toArray(final IntFunction<A[]> generator) {
        return stream.toArray(generator);
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public RdfStream onClose(final Runnable closeHandler) {
        return new DefaultRdfStream(topic(), stream.onClose(closeHandler));
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public Iterator<Triple> iterator() {
        return stream.iterator();
    }

    @Override
    public RdfStream parallel() {
        return new DefaultRdfStream(topic(), stream.parallel());
    }

    @Override
    public RdfStream sequential() {
        return new DefaultRdfStream(topic(), stream.sequential());
    }

    @Override
    public Spliterator<Triple> spliterator() {
        return stream.spliterator();
    }

    @Override
    public RdfStream unordered() {
        return new DefaultRdfStream(topic(), stream.unordered());
    }
}
