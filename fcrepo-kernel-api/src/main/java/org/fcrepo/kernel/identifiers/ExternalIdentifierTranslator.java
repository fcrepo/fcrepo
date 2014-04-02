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
/**
 *
 */

package org.fcrepo.kernel.identifiers;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Translates internal {@link String} identifiers to external {@link Resource}
 * identifiers, by passing the input through the translation chain in one
 * direction or the other, then converting the results into a {@link Resource}.
 * Subclasses should override doRdfForward() and doRdfReverse().
 *
 * @author ajs6f
 * @date Apr 1, 2014
 */
public class ExternalIdentifierTranslator extends IdentifierTranslator<Resource> {

    @Inject
    private List<InternalIdentifierTranslator> translationChain;

    private Converter<String, String> forward = identity();

    private Converter<String, String> reverse = identity();
    @Override
    protected Resource doForward(final String a) {
        return doRdfForward(forward.convert(a));
    }

    @Override
    protected String doBackward(final Resource a) {
        return reverse.convert(doRdfBackward(a));
    }

    protected Resource doRdfForward(final String a) {
        return createResource(a);
    }

    protected String doRdfBackward(final Resource a) {
        return a.toString();
    }

    /**
     * We fold the list of translators once in each direction and store the
     * resulting calculation.
     */
    @PostConstruct
    public void accumulateTranslations() {
        for (final InternalIdentifierTranslator t : translationChain) {
            forward = forward.andThen(t);
        }
        for (final InternalIdentifierTranslator t : Lists.reverse(translationChain)) {
            reverse = reverse.andThen(t.reverse());
        }
    }

    /**
     * @param chain the translation chain to use
     */
    public void setTranslationChain(final List<InternalIdentifierTranslator> chain) {
        this.translationChain = chain;
        accumulateTranslations();
    }

}
