class AdminController < ApplicationController

  def index
    @objects = FedoraConnection.new.list
  end

  def show
    @object = FedoraConnection.new.get(params[:id])
  end
end
