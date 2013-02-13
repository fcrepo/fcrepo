class AdminController < ApplicationController

  def index
    @objects = FedoraConnection.new.list.map{ |o| {name: o.getName.to_s, path: o.getPath.to_s} }
  end
end
