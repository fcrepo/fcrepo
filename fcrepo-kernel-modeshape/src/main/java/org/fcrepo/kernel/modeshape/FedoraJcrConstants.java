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
package org.fcrepo.kernel.modeshape;

/**
 * Convenience class with constants for commonly used JCR types.
 *
 * @author ajs6f
 * @author acoburn
 * @since Apr 25, 2013
 */
public final class FedoraJcrConstants {

    public static final String JCR_LASTMODIFIED = "jcr:lastModified";

    public static final String JCR_LASTMODIFIEDBY = "jcr:lastModifiedBy";

    public static final String JCR_CREATED = "jcr:created";

    public static final String JCR_CREATEDBY = "jcr:createdBy";

    public static final String JCR_FROZEN_NODE = "jcr:frozenNode";

    public static final String FROZEN_NODE = "nt:frozenNode";

    public static final String FROZEN_MIXIN_TYPES = "jcr:frozenMixinTypes";

    public static final String FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";

    public static final String ROOT = "mode:root";

    public static final String VERSIONABLE = "mix:versionable";

    public static final String FIELD_DELIMITER = "\30^^\30";

    private FedoraJcrConstants() {
        // Prevent instantiation
    }
}
