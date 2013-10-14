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

package org.fcrepo.kernel.testutilities;

import static java.util.Arrays.asList;

import java.util.Iterator;
import java.util.List;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Utility class for mocking tests that will return the same cycle of values
 * always.
 *
 * @author ajs6f
 * @date Oct 11, 2013
 * @param <T>
 */
public class UnlimitedAnswer<T> implements Answer<T> {

    final List<T> values;

    Iterator<T> current;

    /**
     * @param values
     */
    public UnlimitedAnswer(final List<T> values) {
        this.values = values;
        current = values.iterator();
    }

    @Override
    public T answer(final InvocationOnMock invocation) throws Throwable {
        // reset if needed
        if (!current.hasNext()) {
            current = values.iterator();
        }
        return current.next();
    }

    /**
     * @param values
     * @return
     */
    public static <F> UnlimitedAnswer<F> always(final List<F> values) {
        return new UnlimitedAnswer<F>(values);
    }

    /**
     * @param vs
     * @return
     */
    @SafeVarargs
    public static <F> UnlimitedAnswer<F> always(final F... vs ) {
        return new UnlimitedAnswer<F>(asList(vs));
    }

}
