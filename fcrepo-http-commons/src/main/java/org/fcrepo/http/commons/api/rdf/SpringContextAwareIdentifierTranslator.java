/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.http.commons.api.rdf;

import java.util.List;

import org.fcrepo.kernel.identifiers.ExternalIdentifierConverter;
import org.fcrepo.kernel.identifiers.InternalIdentifierConverter;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;


/**
 * @author ajs6f
 * @since Apr 3, 2014
 */
public abstract class SpringContextAwareIdentifierTranslator extends ExternalIdentifierConverter implements
        IdentifierTranslator {

    protected List<InternalIdentifierConverter> getTranslationChain() {
        final ApplicationContext context = getApplicationContext();
        if (context != null) {
            @SuppressWarnings("unchecked")
            final List<InternalIdentifierConverter> tchain =
                    getApplicationContext().getBean("translationChain", List.class);
            return tchain;
        }
        return null;
    }

    protected ApplicationContext getApplicationContext() {
        return ContextLoader.getCurrentWebApplicationContext();
    }

}
