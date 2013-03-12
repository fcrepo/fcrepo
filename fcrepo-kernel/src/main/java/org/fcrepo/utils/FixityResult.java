package org.fcrepo.utils;

public class FixityResult {
    public long computedSize;

    public String computedChecksum;

    public boolean equals(Object obj) {

        boolean result = false;
        if (obj instanceof FixityResult) {
            FixityResult that = (FixityResult) obj;
            result = this.computedSize == that.computedSize && this.computedChecksum.equals(that.computedChecksum);

        }

        return result;
    }
}
