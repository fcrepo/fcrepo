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
package org.fcrepo.http.commons.test.util;

import static com.sun.jersey.api.uri.UriComponent.Type.PATH_SEGMENT;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class PathSegmentImpl implements PathSegment {

    // private static final PathSegment EMPTY_PATH_SEGMENT = new
    // PathSegmentImpl("", false);
    private final String path;

    private final MultivaluedMap<String, String> matrixParameters;

    PathSegmentImpl(final String path, final boolean decode) {
        this(path, decode, new MultivaluedMapImpl());
    }

    PathSegmentImpl(final String path, final boolean decode,
            final MultivaluedMap<String, String> matrixParameters) {
        this.path = (decode) ? UriComponent.decode(path, PATH_SEGMENT) : path;
        this.matrixParameters = matrixParameters;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public MultivaluedMap<String, String> getMatrixParameters() {
        return matrixParameters;
    }

    public static List<PathSegment> createPathList(final String... strings) {
        final ArrayList<PathSegment> result =
            new ArrayList<>(strings.length);
        for (final String string : strings) {
            result.add(new PathSegmentImpl(string, false));
        }
        return result;
    }
}
