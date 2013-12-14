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

package org.fcrepo.kernel.rdf.impl.mappings;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import org.slf4j.Logger;
import com.google.common.collect.AbstractIterator;
import com.google.common.base.Function;

/**
 * An {@link Iterator} that zips an Iterator of functions with an iterator of
 * values on which the functions must act.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 * @param <FromType>
 * @param <ToType>
 */
public class ZippingIterator<FromType, ToType> extends AbstractIterator<ToType> {

    private Iterator<FromType> from;

    private Iterator<Function<FromType, ToType>> through;

    private static final Logger LOGGER = getLogger(ZippingIterator.class);

    /**
     * Default constructor.
     *
     * @param from
     * @param through
     */
    public ZippingIterator(final Iterator<FromType> from,
            final Iterator<Function<FromType, ToType>> through) {
        this.from = from;
        this.through = through;
    }

    @Override
    protected ToType computeNext() {
        final boolean hasNext = (from.hasNext() && through.hasNext());
        if (hasNext) {
            LOGGER.trace("Found next element.");
            final FromType f = from.next();
            final Function<FromType, ToType> t = through.next();
            LOGGER.trace("Supplying from next element {} through function {}",
                    f, t);
            return t.apply(f);
        } else {
            return endOfData();
        }

    }

}
