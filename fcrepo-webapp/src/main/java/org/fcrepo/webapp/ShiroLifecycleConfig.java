/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.webapp;

import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Shiro lifecycle processor configuration
 *
 * @author bbpennel
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ShiroLifecycleConfig {
    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    private ShiroLifecycleConfig() {
    }
}
