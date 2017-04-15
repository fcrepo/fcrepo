class Datastream

  extend ActiveModel::Naming

  attr_reader :name, :created

  def initialize(name, params={})
    @name = name
    @created = params[:created]
  end

  def to_param
    name
  end

end
