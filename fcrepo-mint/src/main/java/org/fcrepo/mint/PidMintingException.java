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
package org.fcrepo.mint;

/**
 * Runtime exception when PID minting fails.
 *
 * @since 2015-06-05
 * @author escowles
 */
public class PidMintingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message.
     * @param msg Exception message.
    **/
    public PidMintingException(final String msg) {
        super(msg);
    }

    /**
     * Constructor with cause.
     * @param cause Original exception.
    **/
    public PidMintingException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with message and cause.
     * @param msg Exception message.
     * @param cause Original exception.
    **/
    public PidMintingException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
