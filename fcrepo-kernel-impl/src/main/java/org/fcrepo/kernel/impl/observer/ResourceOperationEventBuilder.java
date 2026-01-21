/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.observer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.impl.util.UserUtil;

/**
 * Converts a ResourceOperation into an Event.
 *
 * @author pwinckles
 */
public class ResourceOperationEventBuilder implements EventBuilder {

    private FedoraId fedoraId;
    private Set<EventType> types;
    private Set<String> resourceTypes;
    private String userID;
    private String userAgent;
    private String baseUrl;
    private Instant date;
    private String userAgentBaseUri;

    /**
     * Creates a new EventBuilder based on an ResourceOperation
     *
     * @param fedoraId the FedoraId the operation is on
     * @param operation the ResourceOperation to create an event for
     * @param userAgentBaseUri the base uri of the user agent, optional
     * @return new builder
     */
    public static ResourceOperationEventBuilder fromResourceOperation(final FedoraId fedoraId,
                                                                      final ResourceOperation operation,
                                                                      final String userAgentBaseUri) {
        final var builder = new ResourceOperationEventBuilder();
        builder.fedoraId = fedoraId;
        builder.date = Instant.now();
        builder.resourceTypes = new HashSet<>();
        builder.userID = operation.getUserPrincipal();
        builder.types = new HashSet<>();
        builder.types.add(mapOperationToEventType(operation));
        builder.userAgentBaseUri = userAgentBaseUri;
        return builder;
    }

    private static EventType mapOperationToEventType(final ResourceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
            case OVERWRITE_TOMBSTONE:
                return EventType.RESOURCE_CREATION;
            case UPDATE:
            case UPDATE_HEADERS:
                return EventType.RESOURCE_MODIFICATION;
            case DELETE:
                return EventType.RESOURCE_DELETION;
            case PURGE:
                return EventType.RESOURCE_PURGE;
            case FOLLOW:
                return EventType.INBOUND_REFERENCE;
            default:
                throw new IllegalStateException(
                        String.format("There is no EventType mapping for ResourceOperation type %s on operation %s",
                                operation.getType(), operation));
        }
    }

    private ResourceOperationEventBuilder() {
        // Intentionally left blank
    }

    @Override
    public EventBuilder merge(final EventBuilder other) {
        if (other == null) {
            return this;
        }

        if (!(other instanceof ResourceOperationEventBuilder)) {
            throw new IllegalStateException(
                    String.format("Cannot merge EventBuilders because they are different types <%s> and <%s>",
                            this.getClass(), other.getClass()));
        }

        final var otherCast = (ResourceOperationEventBuilder) other;

        if (!this.fedoraId.equals(otherCast.fedoraId)) {
            throw new IllegalStateException(
                    String.format("Cannot merge events because they are for different resources: <%s> and <%s>",
                            this, otherCast));
        }

        this.types.addAll(otherCast.types);
        this.resourceTypes.addAll(otherCast.resourceTypes);

        if (this.date.isBefore(otherCast.date)) {
            this.date = otherCast.date;
        }

        return this;
    }

    @Override
    public EventBuilder withResourceTypes(final Set<String> resourceTypes) {
        this.resourceTypes = Objects.requireNonNullElse(resourceTypes, new HashSet<>());
        return this;
    }

    @Override
    public EventBuilder withBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    @Override
    public EventBuilder withUserAgent(final String userAgent) {
        if (userAgent != null && !userAgent.isEmpty()) {
            this.userAgent = (userAgent.contains(" ") ? URLEncoder.encode(userAgent, UTF_8) : userAgent);
        }
        return this;
    }

    @Override
    public Event build() {
        URI userUri = null;
        if (userID != null) {
            userUri = UserUtil.getUserURI(userID, userAgentBaseUri);
        }
        return new EventImpl(fedoraId, types, resourceTypes, userID, userUri, userAgent, baseUrl, date);
    }

    @Override
    public String toString() {
        return "ResourceOperationEventBuilder{" +
                "fedoraId=" + fedoraId +
                ", types=" + types +
                ", resourceTypes=" + resourceTypes +
                ", userID='" + userID + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", date=" + date +
                '}';
    }

}
