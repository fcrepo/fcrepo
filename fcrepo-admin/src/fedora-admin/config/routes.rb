FedoraAdmin::Application.routes.draw do
  root :to => 'admin#index'

  resources :fedora_objects, :controller=>'admin', :path=>'objects' do
    resources :datastreams
  end
end
