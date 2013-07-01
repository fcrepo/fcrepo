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

package org.fcrepo.messaging.legacy;

import static com.google.common.base.Throwables.propagate;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.utils.FedoraTypesUtils.convertDateToXSDString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.fcrepo.utils.FedoraTypesUtils;
import org.slf4j.Logger;

/**
 * Serialize events as ATOM XML messages similar to
 * Fedora 3.x
 */
public class LegacyMethod {

    // TODO Figure out where to get the base url
    private static final String BASE_URL = "http://localhost:8080/rest";

    private static final Properties FEDORA_TYPES = new Properties();

    public static final String FEDORA_ID_SCHEME = "xsd:string";

    public static final String DSID_CATEGORY_LABEL = "fedora-types:dsID";

    public static final String PID_CATEGORY_LABEL = "fedora-types:pid";

    private static final String INGEST_METHOD = "ingest";

    private static final String MODIFY_OBJ_METHOD = "modifyObject";

    private static final String PURGE_OBJ_METHOD = "purgeObject";

    private static final String ADD_DS_METHOD = "addDatastream";

    private static final String MODIFY_DS_METHOD = "modifyDatastream";

    private static final String PURGE_DS_METHOD = "purgeDatastream";

    private static final String[] METHODS = new String[] {INGEST_METHOD,
        MODIFY_OBJ_METHOD, PURGE_OBJ_METHOD, ADD_DS_METHOD,
        MODIFY_DS_METHOD, PURGE_DS_METHOD};

    private static final List<String> METHOD_NAMES = Arrays.asList(METHODS);

    private static final Logger LOGGER = getLogger(LegacyMethod.class);

    private static final String MAP_PROPERTIES =
            "/org/fcrepo/messaging/legacy/map.properties";

    static {
        try (final InputStream is =
                LegacyMethod.class.getResourceAsStream(MAP_PROPERTIES)) {
            FEDORA_TYPES.load(is);
        } catch (final IOException e) {
            // it's in the jar.s
            throw propagate(e);
        }
    }

    private final Entry delegate;

    /**
     * TODO
     * 
     * @param jcrEvent
     * @param resource
     * @throws RepositoryException
     */
    public LegacyMethod(final Event jcrEvent, final Node resource)
        throws RepositoryException {
        this(EntryFactory.newEntry());

        final boolean isDatastreamNode =
                FedoraTypesUtils.isFedoraDatastream.apply(resource);
        final boolean isObjectNode =
                FedoraTypesUtils.isFedoraObject.apply(resource) &&
                        !isDatastreamNode;

        if (isDatastreamNode || isObjectNode) {
            setMethodName(mapMethodName(jcrEvent.getType(), isObjectNode));
            final String returnValue = getReturnValue(jcrEvent, resource);
            setContent(getEntryContent(getMethodName(), returnValue));
            if (isDatastreamNode) {
                setPid(resource.getParent().getName());
                setDsId(resource.getName());
            } else {
                setPid(resource.getName());
            }
        } else {
            setMethodName(null);
        }
        final String userID =
                jcrEvent.getUserID() == null ? "unknown" : jcrEvent.getUserID();
        setUserId(userID);
        setModified(new Date(jcrEvent.getDate()));
    }

    /**
     * TODO
     * 
     * @param atomEntry
     */
    public LegacyMethod(final Entry atomEntry) {
        delegate = atomEntry;
    }

    /**
     * TODO
     * 
     * @param atomEntry
     */
    public LegacyMethod(final String atomEntry) {
        delegate = EntryFactory.parse(new StringReader(atomEntry));
    }

    /**
     * TODO
     * 
     * @return
     */
    public Entry getEntry() {
        return delegate;
    }

    /**
     * TODO
     * 
     * @param content
     */
    public void setContent(final String content) {
        delegate.setContent(content);
    }

    /**
     * TODO
     * 
     * @param val
     */
    public void setUserId(String val) {
        if (val == null) {
            delegate.addAuthor("unknown", null, BASE_URL);
        } else {
            delegate.addAuthor(val, null, BASE_URL);
        }
    }

    /**
     * TODO
     * 
     * @return
     */
    public String getUserID() {
        return delegate.getAuthor().getName();
    }

    /**
     * TODO
     * 
     * @param date
     */
    public void setModified(final Date date) {
        delegate.setUpdated(date);
    }

    /**
     * TODO
     * 
     * @return
     */
    public Date getModified() {
        return delegate.getUpdated();
    }

    /**
     * TODO
     * 
     * @param val
     */
    public void setMethodName(final String val) {
        delegate.setTitle(val).setBaseUri(BASE_URL);
    }

    /**
     * TODO
     * 
     * @return
     */
    public String getMethodName() {
        return delegate.getTitle();
    }

    private void setLabelledCategory(String label, String val) {
        final List<Category> vals = delegate.getCategories(FEDORA_ID_SCHEME);
        Category found = null;
        if (vals != null && !vals.isEmpty()) {
            for (Category c : vals) {
                if (label.equals(c.getLabel())) {
                    found = c.setTerm(val);
                }
            }
        }
        if (found == null) {
            delegate.addCategory(FEDORA_ID_SCHEME, val, label);
        }
    }

    private String getLabelledCategory(String label) {
        final List<Category> categories =
                delegate.getCategories(FEDORA_ID_SCHEME);
        for (final Category c : categories) {
            if (label.equals(c.getLabel())) {
                return c.getTerm();
            }
        }
        return null;
    }

    /**
     * TODO
     * 
     * @param val
     */
    public void setPid(final String val) {
        setLabelledCategory(PID_CATEGORY_LABEL, val);
        delegate.setSummary(val);
    }

    /**
     * TODO
     * 
     * @return
     */
    public String getPid() {
        return getLabelledCategory(PID_CATEGORY_LABEL);
    }

    /**
     * TODO
     * 
     * @param val
     */
    public void setDsId(final String val) {
        setLabelledCategory(DSID_CATEGORY_LABEL, val);
    }

    /**
     * TODO
     * 
     * @return
     */
    public String getDsId() {
        return getLabelledCategory(DSID_CATEGORY_LABEL);
    }

    /**
     * TODO
     * 
     * @param writer
     * @throws IOException
     */
    public void writeTo(final Writer writer) throws IOException {
        delegate.writeTo(writer);
    }

    private static String getEntryContent(final String methodName,
            final String returnVal) {
        final String datatype =
                (String) FEDORA_TYPES.get(methodName + ".datatype");
        return objectToString(returnVal, datatype);
    }

    protected static String objectToString(final String obj,
            final String xsdType) {
        if (obj == null) {
            return "null";
        }
        String term;
        // TODO Most of these types are not yet relevant to FCR4, but we can
        // borrow their serializations as necessary
        // several circumstances yield null canonical names
        switch (xsdType) {
            case "fedora-types:ArrayOfString":
                term = "[UNSUPPORTED" + xsdType + "]";
                break;
            case "xsd:boolean":
                term = obj;
                break;
            case "xsd:nonNegativeInteger":
                term = obj;
                break;
            case "fedora-types:RelationshipTuple":
                term = "[UNSUPPORTED" + xsdType + "]";
                break;
            default:
                term = obj;
                term = term.replaceAll("\"", "'");
                break;
        }

        return term;
    }

    protected static String getReturnValue(final Event jcrEvent,
            final Node jcrNode) throws RepositoryException {
        switch (jcrEvent.getType()) {
            case NODE_ADDED:
                return jcrNode.getName();
            case NODE_REMOVED:
            case PROPERTY_ADDED:
            case PROPERTY_CHANGED:
            case PROPERTY_REMOVED:
                return convertDateToXSDString(jcrEvent.getDate());
            default:
                return null;
        }
    }

    protected static String mapMethodName(final int eventType,
            final boolean isObjectNode) {
        switch (eventType) {
            case NODE_ADDED:
                return isObjectNode ? INGEST_METHOD : ADD_DS_METHOD;
            case NODE_REMOVED:
                return isObjectNode ? PURGE_OBJ_METHOD : PURGE_DS_METHOD;
            case PROPERTY_ADDED:
                return isObjectNode ? MODIFY_OBJ_METHOD : MODIFY_DS_METHOD;
            case PROPERTY_CHANGED:
                return isObjectNode ? MODIFY_OBJ_METHOD : MODIFY_DS_METHOD;
            case PROPERTY_REMOVED:
                return isObjectNode ? MODIFY_OBJ_METHOD : MODIFY_DS_METHOD;
        }
        return null;
    }

    /**
     * TODO
     * 
     * @param jmsMessage
     * @return
     */
    public static boolean canParse(final Message jmsMessage) {
        try {
            return EntryFactory.FORMAT.equals(jmsMessage.getJMSType()) &&
                    METHOD_NAMES.contains(jmsMessage
                            .getStringProperty("methodName"));
        } catch (final JMSException e) {
            LOGGER.info("Could not parse message: {}", jmsMessage);
            throw propagate(e);
        }
    }

}
