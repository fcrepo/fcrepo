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
package org.fcrepo.kernel.observer;

import java.util.HashMap;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Created by ermadmix on 4/2/2015.
 * @author ermadmix
 */
public class FixityEvent {

    private long date;
    private String identifier;
    private HashMap<String,String> info;
    private String path;
    int type;
    String userData;
    String userID;

    /**
     *
     * @return
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     *
     * @param baseURL
     */
    public void setBaseURL(final String baseURL) {
        this.baseURL = baseURL;
    }

    String baseURL;

    /**
     * constructor
     */
    public FixityEvent() { };

    /**
     *
     * @return
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     *
     * @param identifier
     */
    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    /**
     *
     * @return
     */
    public HashMap<String,String> getInfo() {
        return info;
    }

    /**
     *
     * @param info
     */
    public void setInfo(final HashMap info) {
        this.info = info;
    }

    /**
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @param path
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     *
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     *
     * @param type
     */
    public void setType(final int type) {
        this.type = type;
    }

    /**
     *
     * @return
     */
    public String getUserData() {
        return userData;
    }

    /**
     *
     * @param userData
     */
    public void setUserData(final String userData) {
        this.userData = userData;
    }

    /**
     *
     * @return
     */
    public String getUserID() {
        return userID;
    }

    /**
     *
     * @param userID
     */
    public void setUserID(final String userID) {
        this.userID = userID;
    }

    /**
     *
     * @return
     */
    public long getDate() {
        return date;
    }

    /**
     *
     * @param date
     */
    public void setDate(final long date) {
        this.date = date;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
