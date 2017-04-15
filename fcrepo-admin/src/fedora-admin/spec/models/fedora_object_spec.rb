require 'spec_helper'

describe FedoraObject do

  before do
    @java_obj = stub(:java_obj, :getName=>'stub foo', :getPath=>'/objects/test:1')
    @java_obj.stub(:getProperty).and_return(stub('stub_property', :getString=>'stub value'))
  end

  subject do
    FedoraObject.new(@java_obj)
  end

  it "should have datastreams" do
    subject.datastreams.should == "Foo"
  end

end
