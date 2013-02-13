require 'spec_helper'

describe FedoraConnection do
  after do
    subject.stop
  end
  it "should get a list of objects" do
    subject.list.map(&:name).should == ["foo:1", "foo:2"]
  end
end
