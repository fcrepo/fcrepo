/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

/**
 * Standard JMS {@link TextMessage} implementation backed by an in-memory map. Used in unit tests
 * to verify that production code uses only the JMS API and is therefore broker-agnostic
 * (works with both ActiveMQ Classic and Artemis).
 *
 * @author surfrdan
 */
class InMemoryTextMessage implements TextMessage {

    private final Map<String, Object> properties = new HashMap<>();
    private String text;
    private String messageId;
    private long timestamp;
    private byte[] correlationIdBytes;
    private String correlationId;
    private Destination replyTo;
    private Destination destination;
    private int deliveryMode;
    private boolean redelivered;
    private String type;
    private long expiration;
    private long deliveryTime;
    private int priority;

    @Override
    public void setText(final String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getJMSMessageID() {
        return messageId;
    }

    @Override
    public void setJMSMessageID(final String id) {
        this.messageId = id;
    }

    @Override
    public long getJMSTimestamp() {
        return timestamp;
    }

    @Override
    public void setJMSTimestamp(final long t) {
        this.timestamp = t;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        return correlationIdBytes;
    }

    @Override
    public void setJMSCorrelationIDAsBytes(final byte[] bytes) {
        this.correlationIdBytes = bytes;
    }

    @Override
    public void setJMSCorrelationID(final String id) {
        this.correlationId = id;
    }

    @Override
    public String getJMSCorrelationID() {
        return correlationId;
    }

    @Override
    public Destination getJMSReplyTo() {
        return replyTo;
    }

    @Override
    public void setJMSReplyTo(final Destination dest) {
        this.replyTo = dest;
    }

    @Override
    public Destination getJMSDestination() {
        return destination;
    }

    @Override
    public void setJMSDestination(final Destination dest) {
        this.destination = dest;
    }

    @Override
    public int getJMSDeliveryMode() {
        return deliveryMode;
    }

    @Override
    public void setJMSDeliveryMode(final int mode) {
        this.deliveryMode = mode;
    }

    @Override
    public boolean getJMSRedelivered() {
        return redelivered;
    }

    @Override
    public void setJMSRedelivered(final boolean r) {
        this.redelivered = r;
    }

    @Override
    public String getJMSType() {
        return type;
    }

    @Override
    public void setJMSType(final String t) {
        this.type = t;
    }

    @Override
    public long getJMSExpiration() {
        return expiration;
    }

    @Override
    public void setJMSExpiration(final long e) {
        this.expiration = e;
    }

    @Override
    public long getJMSDeliveryTime() {
        return deliveryTime;
    }

    @Override
    public void setJMSDeliveryTime(final long t) {
        this.deliveryTime = t;
    }

    @Override
    public int getJMSPriority() {
        return priority;
    }

    @Override
    public void setJMSPriority(final int p) {
        this.priority = p;
    }

    @Override
    public void clearProperties() {
        properties.clear();
    }

    @Override
    public boolean propertyExists(final String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBooleanProperty(final String name) {
        final Object v = properties.get(name);
        return v != null && Boolean.parseBoolean(v.toString());
    }

    @Override
    public byte getByteProperty(final String name) {
        return ((Number) properties.get(name)).byteValue();
    }

    @Override
    public short getShortProperty(final String name) {
        return ((Number) properties.get(name)).shortValue();
    }

    @Override
    public int getIntProperty(final String name) {
        return ((Number) properties.get(name)).intValue();
    }

    @Override
    public long getLongProperty(final String name) {
        final Object v = properties.get(name);
        return v == null ? 0L : ((Number) v).longValue();
    }

    @Override
    public float getFloatProperty(final String name) {
        return ((Number) properties.get(name)).floatValue();
    }

    @Override
    public double getDoubleProperty(final String name) {
        return ((Number) properties.get(name)).doubleValue();
    }

    @Override
    public String getStringProperty(final String name) {
        final Object v = properties.get(name);
        return v == null ? null : v.toString();
    }

    @Override
    public Object getObjectProperty(final String name) {
        return properties.get(name);
    }

    @Override
    public Enumeration<?> getPropertyNames() {
        return java.util.Collections.enumeration(properties.keySet());
    }

    @Override
    public void setBooleanProperty(final String name, final boolean value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setByteProperty(final String name, final byte value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setShortProperty(final String name, final short value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setIntProperty(final String name, final int value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setLongProperty(final String name, final long value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setFloatProperty(final String name, final float value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setDoubleProperty(final String name, final double value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setStringProperty(final String name, final String value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void setObjectProperty(final String name, final Object value) {
        validateName(name);
        properties.put(name, value);
    }

    @Override
    public void acknowledge() {
        // no-op
    }

    @Override
    public void clearBody() throws JMSException {
        text = null;
    }

    @Override
    public <T> T getBody(final Class<T> c) {
        return c.cast(text);
    }

    @Override
    public boolean isBodyAssignableTo(@SuppressWarnings("rawtypes") final Class c) {
        return text == null || c.isAssignableFrom(String.class);
    }

    /**
     * Mirrors Artemis's stricter property-name rule (must be a valid Java identifier);
     * if production code accidentally uses dots or other invalid characters, this throws.
     */
    private static void validateName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("property name must not be null or empty");
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new IllegalArgumentException("invalid property name: " + name);
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                throw new IllegalArgumentException("invalid property name: " + name);
            }
        }
    }
}
