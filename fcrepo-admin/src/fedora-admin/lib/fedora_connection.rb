require 'java'
# We ought to just have a classpath with this stuff:
#Dir.glob('../../lib/*.jar') { |f| require f }

java_import 'org.fcrepo.services.DatastreamService'
java_import 'org.fcrepo.services.ObjectService'


java_import 'org.springframework.context.ApplicationContext'
java_import 'org.springframework.context.support.ClassPathXmlApplicationContext'

class FedoraConnection

  attr_reader :repo
  def initialize
    spring_file = "config/repo.xml"
    puts "Spring #{spring_file}"
    @ctx = org.springframework.context.support.ClassPathXmlApplicationContext.new(spring_file)
    @repo = @ctx.getBean("modeshapeRepofactory")
    create_sample_objects
  end

  def stop
    @repo = nil

    @ctx.destroy
    @ctx.close
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
      vals << i.nextNode()
    end while i.hasNext()
    session.logout
    vals
  end
end
