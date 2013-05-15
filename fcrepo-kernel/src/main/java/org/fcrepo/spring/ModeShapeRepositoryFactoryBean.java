
package org.fcrepo.spring;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.services.ObjectService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.RepositoryFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

public class ModeShapeRepositoryFactoryBean implements
        FactoryBean<JcrRepository> {

    @Inject
    private JcrRepositoryFactory jcrRepositoryFactory;

    private Resource repositoryConfiguration;

    private JcrRepository repository;

    @PostConstruct
    public void buildRepository() throws RepositoryException, IOException {
        repository =
                (JcrRepository) jcrRepositoryFactory.getRepository(Collections
                        .singletonMap(RepositoryFactory.URL,
                                repositoryConfiguration.getURL()));

        setupInitialNodes();
    }

    private void setupInitialNodes() throws RepositoryException {

        final Session s = repository.login();

        ObjectService objects = ObjectService.get(repository);
        objects.createObject(s, "/objects");
        objects.createObject(s, "/federated");

        s.save();
        s.logout();
    }

    @Override
    public JcrRepository getObject() throws RepositoryException, IOException {
        return repository;
    }

    @Override
    public Class<?> getObjectType() {
        return Repository.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setRepositoryConfiguration(
            final Resource repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

}
