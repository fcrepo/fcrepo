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

    dsid = "modsMetadata"
    create_sample_datastream(session, "/objects/#{pid}/#{dsid}", 'text/xml', "<mods><title>Just kidding</title></mods>")

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
    vals = []
    within_session do |session|
      objects = session.getNode("/objects")
      i = objects.getNodes()
      begin 
        vals << FedoraObject.new(i.nextNode)
      end while i.hasNext
    end
    vals
  end

  def get(id, with_datastreams=true)
    session = @repo.login
    obj = nil
    within_session do |session|
      object = session.getNode("/objects/#{id}")
      ds = with_datastreams ? load_datastreams(object) : nil
      obj = FedoraObject.new(object, ds)
    end
    obj
  end

  def within_session &block
    session = @repo.login
    yield session
    session.logout
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

  def find_datastream(pid, dsid)
    session = @repo.login
    ds = session.getNode("/objects/#{pid}/#{dsid}")
    obj = Datastream.new(ds.getName, created: ds.getProperty("jcr:created").getString)
    session.logout
    obj
  end

  def datastream_content(pid, dsid)
    mimeType = responseStream = nil
    within_session do |session|
      ds = session.getNode("/objects/#{pid}/#{dsid}")
      mimeType = ds.hasProperty("fedora:contentType") ? ds.getProperty( "fedora:contentType").getString() : "application/octet-stream"
      responseStream = ds.getNode(org.modeshape.jcr.api.JcrConstants::JCR_CONTENT).getProperty(org.modeshape.jcr.api.JcrConstants::JCR_DATA).getBinary().getStream()
    end
    return mimeType, responseStream
  end

end
