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
package org.modeshape.jcr;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.jcr.Repository;

import org.modeshape.jcr.value.binary.BinaryStore;

import com.google.common.base.Function;

/**
 * Retrieve the BinaryStore from a running Modeshape Repository
 * @author cbeer
 * @since Apr 30, 2013
 */
public class GetBinaryStore implements Function<Repository, BinaryStore> {

    /**
     * Extract the BinaryStore out of Modeshape
     * (infinspan, jdbc, file, transient, etc)
     * @return the binary store from Modeshape
     */
    @Override
    public BinaryStore apply(final Repository input) {
        checkNotNull(input, "null cannot have a BinaryStore!");
        final JcrRepository.RunningState runningState = ((JcrRepository)input).runningState();

        return runningState.binaryStore();

    }

}
