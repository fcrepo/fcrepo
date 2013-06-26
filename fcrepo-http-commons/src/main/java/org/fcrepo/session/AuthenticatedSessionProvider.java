
package org.fcrepo.session;

import javax.jcr.Session;

public interface AuthenticatedSessionProvider {

    Session getAuthenticatedSession();
}
