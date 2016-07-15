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
package org.fcrepo.kernel.modeshape.testutilities;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.slf4j.Logger;

/**
 * <p>TestNodeIterator class.</p>
 *
 * @author ajs6f
 */
public class TestNodeIterator implements NodeIterator {

    private static final Logger LOGGER = getLogger(TestNodeIterator.class);

    private Iterator<Node> iter;

    private int counter;

    public TestNodeIterator(final Node... nodes) {
        this.counter = 0;
        this.iter = Arrays.asList(nodes).iterator();
    }

    public static TestNodeIterator nodeIterator(final Node... nodes) {
        return new TestNodeIterator(nodes);
    }

    @Override
    public void skip(final long skipNum) {
        for (int i = 0; i < skipNum; i++) {
            iter.next();
        }
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
        iter.remove();
    }

    @Override
    public Node nextNode() {
        counter++;
        final Node n = iter.next();
        LOGGER.debug("Returning node: {}", n);
        return n;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public Object next() {
        return nextNode();
    }

}
