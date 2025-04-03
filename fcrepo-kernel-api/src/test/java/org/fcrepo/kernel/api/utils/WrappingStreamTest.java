/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link WrappingStream}
 *
 * @author whikloj
 */
public class WrappingStreamTest {

    private WrappingStream<String> stream;

    @BeforeEach
    public void setUp() {
        stream = generateStream();
    }

    private WrappingStream<String> generateStream() {
        return new TestWrappingStream(Stream.of("a", "b", "c"));
    }

    @Test
    public void testFilter() {
        // Filter out things matching "a"
        final var filteredStream = stream.filter(s -> !s.equals("a")).collect(Collectors.toList());
        assertEquals(2, filteredStream.size());
        assertTrue(filteredStream.contains("b"));
        assertTrue(filteredStream.contains("c"));
        assertFalse(filteredStream.contains("a"));
    }

    @Test
    public void testAllMatch() {
        // Do all things match "a"?
        final var allMatch = stream.allMatch(s -> s.equals("a"));
        assertFalse(allMatch);
        stream = generateStream();
        // Do all things match "a" or "b" or "c"?
        final var allMatchTrue = stream.allMatch(s -> s.equals("a") || s.equals("b") || s.equals("c"));
        assertTrue(allMatchTrue);
    }

    @Test
    public void testAnyMatch() {
        // Do any things match "a"?
        final var anyMatch = stream.anyMatch(s -> s.equals("a"));
        assertTrue(anyMatch);
        stream = generateStream();
        // Do any things match "c"?
        final var anyMatch2 = stream.anyMatch(s -> s.equals("c"));
        assertTrue(anyMatch2);
        stream = generateStream();
        // Do any things match "d"?
        final var anyMatchFalse = stream.anyMatch(s -> s.equals("d"));
        assertFalse(anyMatchFalse);
    }

    @Test
    public void testCollect() {
        final var collected = stream.collect(Collectors.toList());
        assertEquals(3, collected.size());
        assertTrue(collected.contains("a"));
        assertTrue(collected.contains("b"));
        assertTrue(collected.contains("c"));
        assertInstanceOf(List.class, collected);
    }

    @Test
    public void testCount() {
        final var count = stream.count();
        assertEquals(3, count);
    }

    @Test
    public void testFindAny() {
        // Find any of the elements
        final var any = stream.findAny();
        assertTrue(any.isPresent());
        assertTrue(any.get().equals("a") || any.get().equals("b") || any.get().equals("c"));
        stream = generateStream();
        final var any2 = stream.filter(s -> s.equals("b")).findAny();
        assertTrue(any2.isPresent());
        assertEquals("b", any2.get());
        assertFalse(any2.get().equals("a") || any2.get().equals("c"));
    }

    @Test
    public void testFindFirst() {
        final var first = stream.findFirst();
        assertTrue(first.isPresent());
        assertEquals("a", first.get());
        assertFalse(first.get().equals("b") || first.get().equals("c"));
        stream = generateStream();
        final var first1 = stream.filter(s -> !s.equals("a")).findFirst();
        assertTrue(first1.isPresent());
        assertEquals("b", first1.get());
        assertFalse(first1.get().equals("a") || first1.get().equals("c"));
    }

    @Test
    public void testFlatMap() {
        final var flatMapped = stream.flatMap(s -> Stream.of(s, s.toUpperCase())).collect(Collectors.toList());
        assertEquals(6, flatMapped.size());
        assertTrue(flatMapped.contains("a"));
        assertTrue(flatMapped.contains("A"));
        assertTrue(flatMapped.contains("b"));
        assertTrue(flatMapped.contains("B"));
        assertTrue(flatMapped.contains("c"));
        assertTrue(flatMapped.contains("C"));
    }

    @Test
    public void testFlatMapToDouble() {
        stream = new TestWrappingStream(Stream.of("1.0", "2.0", "3.0"));
        final var flatMapped = stream.flatMapToDouble(s -> DoubleStream.of(Double.parseDouble(s)))
                        .min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1.0, flatMapped.getAsDouble());
    }

    @Test
    public void testFlatMapToInt() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var flatMapped = stream.flatMapToInt(s -> IntStream.of(Integer.parseInt(s)))
                .min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1, flatMapped.getAsInt());
    }

    @Test
    public void testFlatMapToLong() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var flatMapped = stream.flatMapToLong(s -> LongStream.of(Long.parseLong(s)))
                .min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1L, flatMapped.getAsLong());
    }

    @Test
    public void testForEach() {
        final var result = new StringBuilder();
        stream.forEach(result::append);
        assertTrue(
                result.toString().equals("abc") ||
                        result.toString().equals("acb") ||
                        result.toString().equals("bac") ||
                        result.toString().equals("bca") ||
                        result.toString().equals("cab") ||
                        result.toString().equals("cba")
        );
    }

    @Test
    public void testForEachOrdered() {
        stream = new TestWrappingStream(Stream.of("f", "e", "d", "o", "r", "a"));
        final var result = new StringBuilder();
        stream.forEachOrdered(result::append);
        assertEquals("fedora", result.toString());
    }

    @Test
    public void testMap() {
        final var mapped = stream.map(String::toUpperCase).collect(Collectors.toList());
        assertEquals(3, mapped.size());
        assertTrue(mapped.contains("A"));
        assertTrue(mapped.contains("B"));
        assertTrue(mapped.contains("C"));
        assertFalse(mapped.contains("a"));
        assertFalse(mapped.contains("b"));
        assertFalse(mapped.contains("c"));
    }

    @Test
    public void testMapToDouble() {
        stream = new TestWrappingStream(Stream.of("1.0", "2.0", "3.0"));
        final var mapped = stream.mapToDouble(Double::parseDouble).reduce(0, Double::sum);
        assertEquals(6.0, mapped);
    }

    @Test
    public void testMapToInt() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var mapped = stream.mapToInt(Integer::parseInt).reduce(0, Integer::sum);
        assertEquals(6, mapped);
    }

    @Test
    public void testMapToLong() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var mapped = stream.mapToLong(Long::parseLong).reduce(0, Long::sum);
        assertEquals(6L, mapped);
    }

    @Test
    public void testMax() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var max = stream.mapToInt(Integer::parseInt).max();
        assertTrue(max.isPresent());
        assertEquals(3, max.getAsInt());
    }

    @Test
    public void testMin() {
        stream = new TestWrappingStream(Stream.of("1", "2", "3"));
        final var min = stream.mapToInt(Integer::parseInt).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());
    }

    @Test
    public void testNoneMatch() {
        final var noneMatch = stream.noneMatch(s -> s.equals("d"));
        assertTrue(noneMatch);
        stream = generateStream();
        final var noneMatch2 = stream.noneMatch(s -> s.equals("a"));
        assertFalse(noneMatch2);
    }

    static class TestWrappingStream extends WrappingStream<String> {

        public TestWrappingStream(final Stream<String> stream) {
            this.stream = stream;
        }

        @Override
        public Stream<String> filter(final Predicate<? super String> predicate) {
            return stream.filter(predicate);
        }

        @Override
        public Stream<String> distinct() {
            return stream.distinct();
        }

        @Override
        public Stream<String> sorted() {
            return stream.sorted();
        }

        @Override
        public Stream<String> sorted(final Comparator<? super String> comparator) {
            return stream.sorted(comparator);
        }

        @Override
        public Stream<String> peek(final Consumer<? super String> action) {
            return stream.peek(action);
        }

        @Override
        public Stream<String> limit(final long maxSize) {
            return stream.limit(maxSize);
        }

        @Override
        public Stream<String> skip(final long n) {
            return stream.skip(n);
        }

        @Override
        public Stream<String> sequential() {
            return stream.sequential();
        }

        @Override
        public Stream<String> parallel() {
            return stream.parallel();
        }

        @Override
        public Stream<String> unordered() {
            return stream.unordered();
        }

        @Override
        public Stream<String> onClose(final Runnable closeHandler) {
            return stream.onClose(closeHandler);
        }
    }
}
