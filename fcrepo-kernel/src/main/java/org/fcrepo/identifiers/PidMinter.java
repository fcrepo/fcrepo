package org.fcrepo.identifiers;

/**
 * Defines the behavior of a component that can accept responsibility
 * for the creation of Fedora PIDs.
 * 
 * @author ajs6f
 *
 */
public interface PidMinter {
	
    public String mintPid();
		
}
