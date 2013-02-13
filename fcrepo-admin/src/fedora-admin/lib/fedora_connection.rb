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
    obj = ObjectService.new.createObjectNode(session, "/objects/#{pid}");

    dsid = "descMetadata"
    create_sample_datastream(session, "/objects/#{pid}/#{dsid}", 'text/plain', "Sup dood?")
    pid = 'foo:2'
    obj = ObjectService.new.createObjectNode(session, "/objects/#{pid}");

    session.save
    session.logout
  end

  def create_sample_datastream(session, dsPath, contentType, body)
    DatastreamService.new.createDatastreamNode(session, dsPath,
                contentType, java.io.StringBufferInputStream.new(body))
  end

  def list
    session = @repo.login
    objects = session.getNode("/objects")
    i = objects.getNodes()
    vals = []
    begin 
      vals << FedoraObject.new(i.nextNode)
    end while i.hasNext
    session.logout
    vals
  end

  def get(id, with_datastreams=true)
    session = @repo.login
    object = session.getNode("/objects/#{id}")
    ds = with_datastreams ? load_datastreams(object) : nil
    obj = FedoraObject.new(object, ds)
    session.logout
    obj
  end

  def load_datastreams(object)
    i = object.getNodes
    vals = []
    while i.hasNext
      ds = i.nextNode
      vals << Datastream.new(ds.getName)
    end
    vals
  end

end
