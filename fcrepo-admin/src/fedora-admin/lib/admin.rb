require 'java'

# We ought to just have a classpath with this stuff:
Dir.glob('lib/*.jar') { |f| require f }

java_import 'org.fcrepo.services.DatastreamService'
java_import 'org.fcrepo.services.ObjectService'


java_import 'org.springframework.context.ApplicationContext'
java_import 'org.springframework.context.support.ClassPathXmlApplicationContext'

def main
  ctx = org.springframework.context.support.ClassPathXmlApplicationContext.new("spring-test/repo.xml");
  @repo = ctx.getBean("modeshapeRepofactory")

  createSampleObjects
  list
  @repo = nil

  ctx.stop
  ctx.close
end

def createSampleObjects 
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
	begin 
    n = i.nextNode()
    puts n.getName() + " " + n.getPath()
  end while i.hasNext()


  session.logout
end


main
