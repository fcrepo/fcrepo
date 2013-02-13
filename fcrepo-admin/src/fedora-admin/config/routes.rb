FedoraAdmin::Application.routes.draw do
  root :to => 'admin#index'

  resources :fedora_objects, :controller=>'admin', :path=>'objects' do
    resources :datastreams do
      member do
        get "download"
      end
    end
  end
end
