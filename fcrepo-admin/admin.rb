require 'java'

# We ought to just have a classpath with this stuff:
Dir.glob('lib/*.jar') { |f| require f }

java_import 'org.fcrepo.services.DatastreamService'

java_import 'org.springframework.context.ApplicationContext'
java_import 'org.springframework.context.support.ClassPathXmlApplicationContext'

def main
  ctx = org.springframework.context.support.ClassPathXmlApplicationContext.new("spring-test/repo.xml");
  @repo = ctx.getBean("modeshapeRepofactory")

  puts "Before list"
  list
  puts "After list"
  @repo = nil

  ctx.stop
  ctx.close
end

def list
puts "In list"
  session = @repo.login
  objects = session.getNode("/objects")
  puts "\nList of objects:"
  objects.each do |o|
  puts 

  end

  puts "Done"

  session.logout
end


main
