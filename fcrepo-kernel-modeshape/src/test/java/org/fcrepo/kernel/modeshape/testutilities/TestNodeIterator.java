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
package org.fcrepo.kernel.modeshape.testutilities;

import static com.google.common.collect.Iterators.advance;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.slf4j.Logger;

import com.google.common.collect.Iterators;

/**
 * <p>TestNodeIterator class.</p>
 *
 * @author ajs6f
 */
public class TestNodeIterator implements NodeIterator {

    private static final Logger LOGGER = getLogger(TestNodeIterator.class);

    private Iterator<Node> i;

    private int counter;

    public TestNodeIterator(final Node... nodes) {
        this.counter = 0;
        this.i = Iterators.forArray(nodes);
    }

    public static TestNodeIterator nodeIterator(final Node... nodes) {
        return new TestNodeIterator(nodes);
    }

    @Override
    public void skip(final long skipNum) {
        advance(i, (int) skipNum);

    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public long getPosition() {
        return counter;
    }

    @Override
    public void remove() {
        i.remove();
    }

    @Override
    public Node nextNode() {
        counter++;
        final Node n = i.next();
        LOGGER.debug("Returning node: {}", n);
        return n;
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public Object next() {
        return nextNode();
    }

}
