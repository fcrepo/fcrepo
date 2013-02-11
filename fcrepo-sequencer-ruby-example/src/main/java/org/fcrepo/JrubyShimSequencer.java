package org.fcrepo;

import org.modeshape.jcr.api.sequencer.Sequencer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;

public class JrubyShimSequencer extends Sequencer {

    private String rubyFile = "/reverse_content_sequencer.rb";

    @Override
    public boolean execute(Property property, Node node, Context context) throws Exception {


        ScriptEngine jruby = new ScriptEngineManager().getEngineByName("jruby");

        InputStream in = JrubyShimSequencer.class.getResourceAsStream(rubyFile);
        Reader r = new InputStreamReader(in, "UTF-8");

        jruby.eval(new BufferedReader(r));

        JrubySequencerShim s = (JrubySequencerShim) jruby.eval("ReverseContentSequencer.new");

        return s.execute(property, node, context);
    }
}
