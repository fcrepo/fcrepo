require 'java'

java_import 'org.modeshape.jcr.api.sequencer.Sequencer'
java_package 'org.fcrepo'

class FcrepoSequencerRubyExample < Sequencer
  def initialize
  end

  java_signature "boolean execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context)"
  def execute property, outputNode, context
    return true

  end

end
