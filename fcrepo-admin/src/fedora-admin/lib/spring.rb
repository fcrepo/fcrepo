java_import 'org.springframework.context.ApplicationContext'
java_import 'org.springframework.context.support.ClassPathXmlApplicationContext'
class Spring
  include Singleton

  def get(bean_name)
    @repo = context.getBean("modeshapeRepofactory")
  end
 
  def stop
    return unless @ctx
    $stderr.puts "Stopping spring..."
    @ctx.destroy
    @ctx.close
    $stderr.puts "Stopped"
  end

  def context
    spring_file = "config/repo.xml"
    @ctx ||= org.springframework.context.support.ClassPathXmlApplicationContext.new(spring_file)
  end
end

