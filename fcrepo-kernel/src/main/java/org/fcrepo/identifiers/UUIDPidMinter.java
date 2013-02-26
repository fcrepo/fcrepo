package org.fcrepo.identifiers;

import static java.util.UUID.randomUUID;

/**
 * Simple PidMinter that replies on Java's inbuilt UUID minting.
 * 
 * @author ajs6f
 *
 */
public class UUIDPidMinter implements PidMinter {

	@Override
	public String mintPid() {
        return randomUUID().toString();
	}

}
