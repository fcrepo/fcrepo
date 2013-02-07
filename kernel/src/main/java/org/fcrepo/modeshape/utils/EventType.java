package org.fcrepo.modeshape.utils;

public enum EventType {
	NODE_ADDED, NODE_REMOVED, PROPERTY_ADDED, PROPERTY_REMOVED, PROPERTY_CHANGED, NODE_MOVED, PERSIST;

	public static EventType getEventType(final Integer i) {
		switch (i) {
		case 0x1:
			return NODE_ADDED;
		case 0x2:
			return NODE_REMOVED;
		case 0x4:
			return PROPERTY_ADDED;
		case 0x8:
			return PROPERTY_REMOVED;
		case 0x10:
			return PROPERTY_CHANGED;
		case 0x20:
			return NODE_MOVED;
		case 0x40:
			return PERSIST;

		default: // no default
			throw new IllegalArgumentException("Invalid JCR event type: " + i);
		}
	}

	public static String getEventName(final Integer jcrEvent) {

		switch (getEventType(jcrEvent)) {
		case NODE_ADDED:
			return "node added";
		case NODE_REMOVED:
			return "node removed";
		case PROPERTY_ADDED:
			return "property added";
		case PROPERTY_CHANGED:
			return "property changed";
		case PROPERTY_REMOVED:
			return "property removed";
		case NODE_MOVED:
			return "node moved";
		case PERSIST:
			return "persist";

		default: // no default
			throw new IllegalArgumentException("Invalid JCR event type: "
					+ jcrEvent);
		}
	}
}