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

package org.fcrepo.kernel.api.identifiers;

import java.util.Iterator;
import java.util.List;

import static org.fcrepo.kernel.api.functions.InjectiveConverter.identity;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.functions.CompositeInjectiveConverter;
import org.fcrepo.kernel.api.functions.InjectiveConverter;

import org.slf4j.Logger;

/**
 * Wraps a chain of {@link InjectiveConverter}s as a single pipeline.
 *
 * @author ajs6f
 * @param <A> input type
 * @param <B> output type
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ChainWrappingConverter<A, B> implements InjectiveConverter<A, B> {

    private static final Logger log = getLogger(ChainWrappingConverter.class);

    private InjectiveConverter chain = identity();

    @Override
    public A toDomain(final B b) {
        return (A) chain.toDomain(b);
    }

    @Override

    public B apply(final A a) {
        return (B) chain.apply(a);
    }

    @Override
    public InjectiveConverter<B, A> reverse() {
        return chain.reverse();
    }

    @Override
    public boolean inDomain(final A a) {
        return chain.inDomain(a);
    }

    /**
     * @param chain the new list of translators to use
     */
    public void setTranslationChain(final List<InjectiveConverter> chain) {
        log.debug("Using translation chain: {}", chain);
        switch (chain.size()) {
        case 0:
            this.chain = identity();
            break;
        case 1:
            this.chain = chain.get(0);
            break;
        case 2:
            this.chain = new CompositeInjectiveConverter<>(chain.get(0), chain.get(1));
            break;
        default:
            final Iterator<InjectiveConverter> converters = chain.iterator();
            this.chain = new CompositeInjectiveConverter<>(converters.next(), converters.next());
            converters.forEachRemaining(this::add);
        }
    }

    protected void add(final InjectiveConverter addend) {
        chain = chain.andThen(addend);
    }
}
