require 'spec_helper'

require 'lib/fedora_connection'

describe FedoraConnection do
  after do
    subject.stop
  end
  it "should get a list of objects" do
    subject.list.map{ |o| o.getName.to_s }.should == ["foo:1", "foo:2"]
  end
end
