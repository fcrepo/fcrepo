package org.fcrepo;


public interface JrubySequencerShim {
    public boolean execute(javax.jcr.Property property, javax.jcr.Node node, org.modeshape.jcr.api.sequencer.Sequencer.Context context);
}
