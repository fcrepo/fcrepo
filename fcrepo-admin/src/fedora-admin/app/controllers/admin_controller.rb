class AdminController < ApplicationController

  def index
    @objects = FedoraConnection.new.list
  end
end
