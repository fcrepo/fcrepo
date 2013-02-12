package org.fcrepo.merritt.checkm;

import org.apache.cxf.helpers.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    List<Entry> entries;

    public Reader(InputStream is) throws IOException {
        this.entries = parseManifest(IOUtils.readStringFromStream(is));
    }

    private List<Entry> parseManifest(String manifest_body) {
        ArrayList<Entry> m = new ArrayList<Entry>();

        String[] rows = manifest_body.split("\n");

        for(String row : rows) {
            if(row.startsWith("#")) {
                continue;
            }

            m.add(new Entry(row.split("|")));
        }


        return m;
    }

    public List<Entry> getEntries() {
        return entries;
    }


}
