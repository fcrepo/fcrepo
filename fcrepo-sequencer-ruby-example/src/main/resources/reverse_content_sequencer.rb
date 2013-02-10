require 'java'

java_import 'org.modeshape.jcr.api.sequencer.Sequencer'
java_package 'org.fcrepo'
java_import org.modeshape.jcr.api.JcrTools


class ReverseContentSequencer
  include org.fcrepo.JrubySequencerShim;

  def execute property, outputNode, context

    n = outputNode.addNode("reversed-content", "nt:resource")
    n.setProperty("jcr:data", property.getString().reverse )

    return true
  end

end
