class DatastreamsController < ApplicationController
  def show
    @datastream = FedoraConnection.new.find_datastream(params[:fedora_object_id], params[:id])
  end

  def download
    mime, stream = FedoraConnection.new.datastream_content(params[:fedora_object_id], params[:id])
    out = java.io.ByteArrayOutputStream.new

    buffer = Java::byte[1024].new
    len = stream.read(buffer)
    while len != -1
      out.write(buffer, 0, len) #copy streams
      len = stream.read(buffer)
    end

    send_data out.toByteArray, type: mime, disposition: 'inline'
  end
end
