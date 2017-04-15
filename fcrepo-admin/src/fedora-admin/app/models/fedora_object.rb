class FedoraObject

  extend ActiveModel::Naming

  attr_reader :name, :label, :path, :owner, :created, :modified, :datastreams

  def initialize(java_object, datastreams=[])
    @name = java_object.getName.to_s
    @label = @name
    @path = java_object.getPath.to_s
    @owner = java_object.getProperty("fedora:ownerId").getString
    @created = java_object.getProperty("jcr:created").getString
    @modified = java_object.getProperty("jcr:lastModified").getString
    @datastreams = datastreams
  end


  def to_param
    name
  end
end
