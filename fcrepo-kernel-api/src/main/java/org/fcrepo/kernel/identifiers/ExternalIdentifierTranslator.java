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

    private Converter<String, String> accumulatedForwardTranslator;

    private Converter<String, String> accumulatedReverseTranslator;

    @Override
    protected Resource doForward(final String a) {
        return doRdfForward(accumulatedForwardTranslator.convert(a));
    }

    @Override
    protected String doBackward(final Resource a) {
        return accumulatedReverseTranslator.convert(doRdfBackward(a));
    }

    protected Resource doRdfForward(final String a) {
        return createResource(a);
    }

    protected String doRdfBackward(final Resource a) {
        return a.toString();
    }

    /**
     * We accumulate the translators once and store the resulting calculation.
     */
    @PostConstruct
    public void simpleFoldLikeAccumulation() {
        Converter<String, String> accumulatedForward = identity();
        for (final InternalIdentifierTranslator t : translationChain) {
            accumulatedForward = accumulatedForward.andThen(t);
        }
        accumulatedForwardTranslator = accumulatedForward;
        Converter<String, String> accumulatedReverse = identity();
        for (final InternalIdentifierTranslator t : Lists.reverse(translationChain)) {
            accumulatedReverse = accumulatedReverse.andThen(t.reverse());
        }
        accumulatedReverseTranslator = accumulatedReverse;
    }


    /**
     * @param translationChain the translationChain to use
     */
    public void setTranslationChain(final List<InternalIdentifierTranslator> chain) {
        this.translationChain = chain;
        simpleFoldLikeAccumulation();
    }

}
