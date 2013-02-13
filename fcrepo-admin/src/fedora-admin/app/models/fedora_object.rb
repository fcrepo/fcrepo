class FedoraObject

  def initialize(java_object)
    @java_object = java_object
  end

  def name
    @java_object.getName.to_s
  end

  def path
    @java_object.getPath.to_s
  end

  def model_name
    'fedora_object'
  end
end
