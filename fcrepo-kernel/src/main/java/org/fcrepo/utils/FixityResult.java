package org.fcrepo.utils;

import java.net.URI;

public class FixityResult {
    public long computedSize;

    public URI computedChecksum;

    public boolean equals(Object obj) {

        boolean result = false;
        if (obj instanceof FixityResult) {
            FixityResult that = (FixityResult) obj;
            result = this.computedSize == that.computedSize && this.computedChecksum.equals(that.computedChecksum);

        }

        return result;
    }
}
