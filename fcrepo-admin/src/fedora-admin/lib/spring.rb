java_import 'org.springframework.context.ApplicationContext'
java_import 'org.springframework.context.support.ClassPathXmlApplicationContext'
class Spring
  include Singleton

  def get(bean_name)
    @repo = @ctx.getBean("modeshapeRepofactory")
  end
 
  def stop
    $stderr.puts "Stopping spring..."
    @ctx.destroy
    @ctx.close
    $stderr.puts "Stopped"
  end

  def initialize
    spring_file = "config/repo.xml"
    puts "Spring #{spring_file}"
    @ctx = org.springframework.context.support.ClassPathXmlApplicationContext.new(spring_file)
  end
end

