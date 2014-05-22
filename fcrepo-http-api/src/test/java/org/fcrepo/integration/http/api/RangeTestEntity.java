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
package org.fcrepo.integration.http.api;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import org.apache.http.entity.InputStreamEntity;

/**
 * Entity for testing range queries.
 * @author Esme Cowles
 * @since 2014-05-13
**/
public class RangeTestEntity extends InputStreamEntity {
    public RangeTestEntity( long size, byte[] data ) {
        super(new RangeTestInputStream(size,data), size + data.length, APPLICATION_OCTET_STREAM);
    }
	public boolean isRepeatable() {
        return true;
    }
}
