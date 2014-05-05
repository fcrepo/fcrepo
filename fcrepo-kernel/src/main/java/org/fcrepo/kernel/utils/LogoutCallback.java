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
package org.fcrepo.kernel.utils;

import static com.google.common.base.Throwables.propagate;

import javax.jcr.Session;

import com.google.common.util.concurrent.FutureCallback;

/**
 * A {@link FutureCallback} intended for use with streaming outputs.
 * It simply logs out the {@link Session} involved.
 *
 * @author ajs6f
 * @date Oct 30, 2013
 */
public class LogoutCallback implements FutureCallback<Void> {

    final Session session;

    /**
     * Normal constructor.
     *
     * @param session
     */
    public LogoutCallback(final Session session) {
        this.session = session;
    }

    @Override
    public void onSuccess(final Void finishedMarker) {
        if (session != null) {
            session.logout();
        }

    }

    @Override
    public void onFailure(final Throwable t) {
        if (session != null) {
            session.logout();
        }
        propagate(t);

    }

}
