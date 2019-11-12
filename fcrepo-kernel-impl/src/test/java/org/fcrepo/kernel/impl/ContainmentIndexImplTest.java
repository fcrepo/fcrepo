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
package org.fcrepo.kernel.impl;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainmentIndexImplTest {
    @Mock
    private FedoraResource parent;

    @Mock
    private FedoraResource child;

    @Mock
    private Transaction transaction;

    @Inject
    private ContainmentIndexImpl containmentIndex;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void testAddChild() {
        when(parent.getId()).thenReturn("A");
        when(child.getId()).thenReturn("B");
        assertEquals(0, containmentIndex.getContainedBy(null, parent).count());
        containmentIndex.addContainedBy(null, parent, child);
        assertEquals(1, containmentIndex.getContainedBy(null, parent).count());
        assertEquals(child.getId(), containmentIndex.getContainedBy(null, parent).findFirst().get());
    }

    @Test
    public void testRemoveChild() {
        when(parent.getId()).thenReturn("C");
        when(child.getId()).thenReturn("D");
        containmentIndex.addContainedBy(null, parent, child);
        assertEquals(1, containmentIndex.getContainedBy(null, parent).count());
        containmentIndex.removeContainedBy(null, parent, child);
        assertEquals(0, containmentIndex.getContainedBy(null, parent).count());
    }

    @Test
    public void testAddChildInTransaction() {
        when(parent.getId()).thenReturn("E");
        when(child.getId()).thenReturn("F");
        when(transaction.getId()).thenReturn("foo");
        assertEquals(0, containmentIndex.getContainedBy(null, parent).count());
        containmentIndex.addContainedBy(transaction, parent, child);
        assertEquals(1, containmentIndex.getContainedBy(transaction, parent).count());
        assertEquals(child.getId(), containmentIndex.getContainedBy(transaction, parent).findFirst().get());
        // outside of the transaction, the containment shouldn't show up
        assertEquals(0, containmentIndex.getContainedBy(null, parent).count());
        containmentIndex.removeContainedBy(transaction, parent, child);
        assertEquals(0, containmentIndex.getContainedBy(transaction, parent).count());
    }

    @Test
    public void testRemoveChildInTransaction() {
        when(parent.getId()).thenReturn("G");
        when(child.getId()).thenReturn("H");
        when(transaction.getId()).thenReturn("bar");
        assertEquals(0, containmentIndex.getContainedBy(transaction, parent).count());
        containmentIndex.addContainedBy(transaction, parent, child);
        assertEquals(1, containmentIndex.getContainedBy(transaction, parent).count());
        containmentIndex.removeContainedBy(transaction, parent, child);
        assertEquals(0, containmentIndex.getContainedBy(transaction, parent).count());
    }
}
