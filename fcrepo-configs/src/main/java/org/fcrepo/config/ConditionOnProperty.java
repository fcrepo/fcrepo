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

package org.fcrepo.config;

import java.util.Objects;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * This condition enables a bean/configuration when the specified property matches the expected value
 *
 * Implementations must provide a no-arg constructor.
 *
 * @author pwinckles
 */
public abstract class ConditionOnProperty<T> implements Condition {

    private final String name;
    private final T expected;
    private final T defaultValue;
    private final Class<T> clazz;

    public ConditionOnProperty(final String name, final T expected, final T defaultValue, final Class<T> clazz) {
        this.name = name;
        this.expected = expected;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
    }

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        return Objects.equals(expected, context.getEnvironment().getProperty(name, clazz, defaultValue));
    }

}
