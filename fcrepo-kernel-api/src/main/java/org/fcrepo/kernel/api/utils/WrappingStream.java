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
package org.fcrepo.kernel.api.utils;

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

/**
 * Partial Implementation of a Wrapping Stream
 *
 * @author acoburn
 * @since Dec 6, 2015
 */
public abstract class WrappingStream<T> implements Stream<T> {

    protected Stream<T> stream;

    @Override
    public boolean allMatch(final Predicate<? super T> predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean anyMatch(final Predicate<? super T> predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public <R, A> R collect(final Collector<? super T, A, R> collector) {
        return stream.collect(collector);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator,
            final BiConsumer<R,R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public Optional<T> findAny() {
        return stream.findAny();
    }

    @Override
    public Optional<T> findFirst() {
        return stream.findFirst();
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) {
        return stream.flatMap(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) {
        return stream.flatMapToDouble(mapper);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) {
        return stream.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) {
        return stream.flatMapToLong(mapper);
    }

    @Override
    public void forEach(final Consumer<? super T> action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(final Consumer<? super T> action) {
        stream.forEachOrdered(action);
    }

    @Override
    public <R> Stream<R> map(final Function<? super T,? extends R> mapper) {
        return stream.map(mapper);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) {
        return stream.mapToDouble(mapper);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) {
        return stream.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) {
        return stream.mapToLong(mapper);
    }

    @Override
    public Optional<T> max(final Comparator<? super T> comparator) {
        return stream.max(comparator);
    }

    @Override
    public Optional<T> min(final Comparator<? super T> comparator) {
        return stream.min(comparator);
    }

    @Override
    public boolean noneMatch(final Predicate<? super T> predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) {
        return stream.reduce(accumulator);
    }

    @Override
    public T reduce(final T identity, final BinaryOperator<T> accumulator) {
        return stream.reduce(identity, accumulator);
    }

    @Override
    public <U> U reduce(final U identity, final BiFunction<U,? super T,U> accumulator,
            final BinaryOperator<U> combiner) {
        return stream.reduce(identity, accumulator, combiner);
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
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public Iterator<T> iterator() {
        return stream.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream.spliterator();
    }
}
