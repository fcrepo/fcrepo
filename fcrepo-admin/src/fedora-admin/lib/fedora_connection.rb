require 'java'

java_import 'org.fcrepo.services.DatastreamService'
java_import 'org.fcrepo.services.ObjectService'

class FedoraConnection

  attr_reader :repo
  def initialize
    @repo = Spring.instance.get('modeshapeRepofactory')
    create_sample_objects
  end

  def stop
    $stderr.puts "Stopping fedora connection and spring..."
    @repo = nil
    @ctx.destroy
    @ctx.close
    $stderr.puts "Stopped"
  end

  def create_sample_objects 
    session = @repo.login
    session.getWorkspace().getNamespaceRegistry().registerNamespace('foo', 'http://example.com/');

    pid = 'foo:1'
    obj = ObjectService.new.createObjectNode(session, "/objects/" + pid);
    pid = 'foo:2'
    obj = ObjectService.new.createObjectNode(session, "/objects/" + pid);

    session.save
    session.logout
  end

  def list
    session = @repo.login
    objects = session.getNode("/objects")
    i = objects.getNodes()
    vals = []
    begin 
      vals << FedoraObject.new(i.nextNode())
    end while i.hasNext()
    session.logout
    vals
  end

  def get(id)
    session = @repo.login
    object = session.getNode("/objects/#{id}")
    obj = FedoraObject.new(object)
    session.logout
    obj
  end

end
