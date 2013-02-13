class Datastream

  extend ActiveModel::Naming

  attr_reader :name

  def initialize(name)
    @name = name
  end

  def to_param
    name
  end

end
