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

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link WrappingStream}
 *
 * @author whikloj
 */
public class WrappingStreamTest {

    protected WrappingStream<Triple> stream;

    protected Node subject = NodeFactory.createURI("http://example.org/subject");
    protected Node predicate = NodeFactory.createURI("http://example.org/predicate");

    protected Node objectA = NodeFactory.createLiteralString("a");
    protected Node objectB = NodeFactory.createLiteralString("b");
    protected Node objectC = NodeFactory.createLiteralString("c");

    protected Node objectFloat1 = NodeFactory.createLiteralString("1.0");
    protected Node objectFloat2 = NodeFactory.createLiteralString("2.0");
    protected Node objectFloat3 = NodeFactory.createLiteralString("3.0");

    protected Node objectInt1 = NodeFactory.createLiteralString("1");
    protected Node objectInt2 = NodeFactory.createLiteralString("2");
    protected Node objectInt3 = NodeFactory.createLiteralString("3");

    @BeforeEach
    public void setUp() {
        stream = generateTextStream();
    }

    /**
     * @return Generate a stream of triples with text literals
     */
    protected WrappingStream<Triple> generateTextStream() {
        return new TestWrappingStream(Stream.of(
                Triple.create(subject, predicate, objectA),
                Triple.create(subject, predicate, objectB),
                Triple.create(subject, predicate, objectC)
        ));
    }

    /**
     * @return Generate a stream of triples with text literals that can be parsed as floats
     */
    protected WrappingStream<Triple> generateFloatStream() {
        return new TestWrappingStream(Stream.of(
            Triple.create(subject, predicate, objectFloat1),
            Triple.create(subject, predicate, objectFloat2),
            Triple.create(subject, predicate, objectFloat3)
        ));
    }

    /**
     * @return Generate a stream of triples with text literals that can be parsed as ints
     */
    protected WrappingStream<Triple> generateIntStream() {
        return new TestWrappingStream(Stream.of(
            Triple.create(subject, predicate, objectInt1),
            Triple.create(subject, predicate, objectInt2),
            Triple.create(subject, predicate, objectInt3)
        ));
    }

    @Test
    public void testFilter() {
        // Filter out things matching "a"
        final var filteredStream = stream.map(Triple::getObject)
                .filter(s -> !s.equals(objectA))
                .toList();
        assertEquals(2, filteredStream.size());
        assertTrue(filteredStream.contains(objectB));
        assertTrue(filteredStream.contains(objectC));
        assertFalse(filteredStream.contains(objectA));
    }

    @Test
    public void testAllMatch() {
        // Do all things match "a"?
        final var allMatch = stream.allMatch(s -> s.getObject().equals(objectA));
        assertFalse(allMatch);
        stream = generateTextStream();
        // Do all things match "a" or "b" or "c"?
        final var allMatchTrue = stream
                .allMatch(s -> s.getObject().equals(objectA) ||
                        s.getObject().equals(objectB) || s.getObject().equals(objectC));
        assertTrue(allMatchTrue);
    }

    @Test
    public void testAnyMatch() {
        // Do any things match "a"?
        final var anyMatch = stream.anyMatch(s -> s.getObject().equals(objectA));
        assertTrue(anyMatch);
        stream = generateTextStream();
        // Do any things match "c"?
        final var anyMatch2 = stream.anyMatch(s -> s.getObject().equals(objectC));
        assertTrue(anyMatch2);
        stream = generateTextStream();
        // Do any things match "d"?
        final var anyMatchFalse = stream
                .anyMatch(s -> s.getObject().equals(NodeFactory.createLiteralString("d")));
        assertFalse(anyMatchFalse);
    }

    @Test
    public void testCollect() {
        final var collected = stream.collect(Collectors.toList());
        assertEquals(3, collected.size());
        assertInstanceOf(List.class, collected);
    }

    @Test
    public void testCollectExtended() {
        final var result = stream.collect(
                StringBuilder::new,  // Supplier
                (sb, t) -> sb.append(t.getObject().getLiteralValue()),  // Accumulator
                StringBuilder::append   // Combiner (for parallelism)
        );
        assertEquals("abc", result.toString());
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
        assertTrue(
                any.get().getObject().equals(objectA) ||
                any.get().getObject().equals(objectB) ||
                any.get().getObject().equals(objectC)
        );
        stream = generateTextStream();
        final var any2 = stream.filter(s -> s.getObject().equals(objectB)).findAny();
        assertTrue(any2.isPresent());
        assertEquals(objectB, any2.get().getObject());
        assertFalse(any2.get().getObject().equals(objectA) || any2.get().getObject().equals(objectC));
    }

    @Test
    public void testFindFirst() {
        final var first = stream.findFirst();
        assertTrue(first.isPresent());
        assertEquals(objectA, first.get().getObject());
        assertFalse(first.get().getObject().equals(objectB) || first.get().getObject().equals(objectC));
    }

    @Test
    public void testFindFirstButNotFirst() {
        final var first1 = stream.filter(s -> !s.getObject().equals(objectA)).findFirst();
        assertTrue(first1.isPresent());
        assertEquals(objectB, first1.get().getObject());
        assertFalse(first1.get().getObject().equals(objectA) || first1.get().getObject().equals(objectC));
    }

    @Test
    public void testFlatMap() {
        final var flatMapped = stream
                .flatMap(s ->
                        Stream.of(
                                s.getObject(),
                                NodeFactory.createLiteralString(
                                        s.getObject().getLiteralValue().toString().toUpperCase()
                                )
                        ))
                .toList();
        assertEquals(6, flatMapped.size());
        assertTrue(flatMapped.contains(objectA));
        assertTrue(flatMapped.contains(NodeFactory.createLiteralString("A")));
        assertTrue(flatMapped.contains(objectB));
        assertTrue(flatMapped.contains(NodeFactory.createLiteralString("B")));
        assertTrue(flatMapped.contains(objectC));
        assertTrue(flatMapped.contains(NodeFactory.createLiteralString("C")));
    }

    @Test
    public void testFlatMapToDouble() {
        stream = generateFloatStream();
        final var flatMapped = stream
                .flatMapToDouble(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return DoubleStream.of(Double.parseDouble(o));
                }).min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1.0, flatMapped.getAsDouble());
    }

    @Test
    public void testFlatMapToInt() {
        stream = generateIntStream();
        final var flatMapped = stream
                .flatMapToInt(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return IntStream.of(Integer.parseInt(o));
                })
                .min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1, flatMapped.getAsInt());
    }

    @Test
    public void testFlatMapToLong() {
        stream = generateIntStream();
        final var flatMapped = stream
                .flatMapToLong(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return LongStream.of(Long.parseLong(o));
                })
                .min().stream().findFirst();
        assertTrue(flatMapped.isPresent());
        assertEquals(1L, flatMapped.getAsLong());
    }

    @Test
    public void testForEach() {
        final var result = new StringBuilder();
        stream.forEach(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    result.append(o);
                });
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
        final var result = new StringBuilder();
        stream.forEachOrdered(s -> {
            final var o = s.getObject().getLiteralValue().toString();
            result.append(o);
        });
        assertEquals("abc", result.toString());
    }

    @Test
    public void testMap() {
        final var mapped = stream.map(Triple::getObject)
                .map(Node::getLiteralValue).map(Object::toString)
                .map(String::toUpperCase).toList();
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
        stream = generateFloatStream();
        final var mapped = stream
                .mapToDouble(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return Double.parseDouble(o);
                }).reduce(0, Double::sum);
        assertEquals(6.0, mapped);
    }

    @Test
    public void testMapToInt() {
        stream = generateIntStream();
        final var mapped = stream
                .mapToInt(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return Integer.parseInt(o);
                }).reduce(0, Integer::sum);
        assertEquals(6, mapped);
    }

    @Test
    public void testMapToLong() {
        stream = generateIntStream();
        final var mapped = stream
                .mapToLong(s -> {
                    final var o = s.getObject().getLiteralValue().toString();
                    return Long.parseLong(o);
                }).reduce(0, Long::sum);
        assertEquals(6L, mapped);
    }

    @Test
    public void testMax() {
        stream = generateIntStream();
        final var max = stream.max((s1, s2) -> {
            final var o1 = Integer.parseInt(s1.getObject().getLiteralValue().toString());
            final var o2 = Integer.parseInt(s2.getObject().getLiteralValue().toString());
            return o1 - o2;
        });
        assertTrue(max.isPresent());
        assertEquals(objectInt3, max.get().getObject());
    }

    @Test
    public void testMin() {
        stream = generateIntStream();
        final var min = stream.min((s1, s2) -> {
            final var o1 = Integer.parseInt(s1.getObject().getLiteralValue().toString());
            final var o2 = Integer.parseInt(s2.getObject().getLiteralValue().toString());
            return o1 - o2;
        });
        assertTrue(min.isPresent());
        assertEquals(objectInt1, min.get().getObject());
        stream = generateTextStream();
        final var min2 = stream.min((s1, s2) -> {
            final var o1 = s1.getObject().getLiteralValue().toString();
            final var o2 = s2.getObject().getLiteralValue().toString();
            return o1.compareTo(o2);
        });
        assertTrue(min2.isPresent());
        assertEquals(objectA, min2.get().getObject());
    }

    @Test
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(s -> s.getObject().equals(NodeFactory.createLiteralString("d"))));
        stream = generateTextStream();
        assertFalse(stream.noneMatch(s -> s.getObject().equals(objectA)));
    }

    @Test
    public void testToArray() {
        final var array = stream.toArray();
        assertEquals(3, array.length);
        assertInstanceOf(Triple.class, array[0]);
        assertInstanceOf(Triple.class, array[1]);
        assertInstanceOf(Triple.class, array[2]);
    }

    @Test
    public void testToArrayGenerator() {
        final var array = stream.toArray(Triple[]::new);
        assertEquals(3, array.length);
        assertInstanceOf(Triple.class, array[0]);
        assertInstanceOf(Triple.class, array[1]);
        assertInstanceOf(Triple.class, array[2]);
    }

    @Test
    public void testReduceIdentity() {
        final var reduced = stream.reduce(Triple.create(subject, predicate, NodeFactory.createLiteralStringString("")),
                (s1, s2) -> Triple.create(s1.getSubject(), s1.getPredicate(),
                        NodeFactory.createLiteralStringString(
                                s1.getObject().getLiteralValue().toString() +
                                        s2.getObject().getLiteralValue().toString()
                        )));
        assertEquals("abc", reduced.getObject().getLiteralValue().toString());
    }

    @Test
    public void testReduce() {
        final var reduced = stream.reduce(
                (s1, s2) -> Triple.create(s1.getSubject(), s1.getPredicate(),
                        NodeFactory.createLiteralStringString(
                                s1.getObject().getLiteralValue().toString() +
                                        s2.getObject().getLiteralValue().toString()
                        )));
        final var reducedString = reduced.map(s -> s.getObject().getLiteralValue().toString());
        assertEquals("abc", reducedString.orElse(""));
    }

    @Test
    public void testReduceWithCombiner() {
        final var identity = Triple.create(subject, predicate, NodeFactory.createLiteralStringString(""));

        final var reduced = stream.reduce(
                identity,
                (s1, s2) -> Triple.create(s1.getSubject(), s1.getPredicate(),
                        NodeFactory.createLiteralStringString(
                                s1.getObject().getLiteralValue().toString() +
                                        s2.getObject().getLiteralValue().toString()
                        )),
                (t1, t2) -> Triple.create(t1.getSubject(), t1.getPredicate(),
                        NodeFactory.createLiteralStringString(
                                t1.getObject().getLiteralValue().toString() +
                                        t2.getObject().getLiteralValue().toString()
                        ))
        );

        assertEquals("abc", reduced.getObject().getLiteralValue().toString());
    }

    @Test
    public void testIterator() {
        final var iterator = stream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(objectA, iterator.next().getObject());
        assertTrue(iterator.hasNext());
        assertEquals(objectB, iterator.next().getObject());
        assertTrue(iterator.hasNext());
        assertEquals(objectC, iterator.next().getObject());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSplitIterator() {
        final var iterator = stream.spliterator();
        assertTrue(iterator.hasCharacteristics(ORDERED));
        assertTrue(iterator.hasCharacteristics(SIZED));
        assertEquals(3, iterator.estimateSize());
        assertEquals(3, iterator.getExactSizeIfKnown());
    }

    @Test
    public void testIsParallel() {
        assertFalse(stream.isParallel());
        final var stream2 = stream.parallel();
        assertTrue(stream2.isParallel());
    }

    static class TestWrappingStream extends WrappingStream<Triple> {

        public TestWrappingStream(final Stream<Triple> stream) {
            this.stream = stream;
        }

        @Override
        public Stream<Triple> filter(final Predicate<? super Triple> predicate) {
            return stream.filter(predicate);
        }

        @Override
        public Stream<Triple> distinct() {
            return stream.distinct();
        }

        @Override
        public Stream<Triple> sorted() {
            return stream.sorted();
        }

        @Override
        public Stream<Triple> sorted(final Comparator<? super Triple> comparator) {
            return stream.sorted(comparator);
        }

        @Override
        public Stream<Triple> peek(final Consumer<? super Triple> action) {
            return stream.peek(action);
        }

        @Override
        public Stream<Triple> limit(final long maxSize) {
            return stream.limit(maxSize);
        }

        @Override
        public Stream<Triple> skip(final long n) {
            return stream.skip(n);
        }

        @Override
        public Stream<Triple> sequential() {
            return stream.sequential();
        }

        @Override
        public Stream<Triple> parallel() {
            return stream.parallel();
        }

        @Override
        public Stream<Triple> unordered() {
            return stream.unordered();
        }

        @Override
        public Stream<Triple> onClose(final Runnable closeHandler) {
            return stream.onClose(closeHandler);
        }
    }
}
