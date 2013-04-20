
package org.fcrepo.identifiers;

import com.google.common.base.Function;

/**
 * Defines the behavior of a component that can accept responsibility
 * for the creation of Fedora PIDs. Do not implement this interface directly.
 * Subclass {@link BasePidMinter} instead.
 * 
 * @author ajs6f
 *
 */
public interface PidMinter {

    public String mintPid();

    public Function<Object, String> makePid();

}
