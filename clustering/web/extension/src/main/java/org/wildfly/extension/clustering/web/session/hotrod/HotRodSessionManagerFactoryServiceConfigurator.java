/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.clustering.web.session.hotrod;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactory;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.hotrod.session.SessionManagerNearCacheFactory;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SpecificationProvider;

/**
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <MC> the marshalling context type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class HotRodSessionManagerFactoryServiceConfigurator<S, SC, AL, LC>  extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC>, Supplier<SessionManagerFactory<SC, LC, TransactionBatch>> {

    private final HotRodSessionManagementConfiguration<DeploymentUnit> configuration;
    private final SessionManagerFactoryConfiguration<S, SC, AL, LC> factoryConfiguration;

    private volatile ServiceConfigurator cacheConfigurator;
    @SuppressWarnings("rawtypes")
    private volatile SupplierDependency<RemoteCache> cache;

    public HotRodSessionManagerFactoryServiceConfigurator(HotRodSessionManagementConfiguration<DeploymentUnit> configuration, SessionManagerFactoryConfiguration<S, SC, AL, LC> factoryConfiguration) {
        super(ServiceName.JBOSS.append("clustering", "web", factoryConfiguration.getDeploymentName()));
        this.configuration = configuration;
        this.factoryConfiguration = factoryConfiguration;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        Integer maxActiveSessions = this.getMaxActiveSessions();
        NearCacheMode mode = (maxActiveSessions != null) && (maxActiveSessions > 0) ? NearCacheMode.INVALIDATED : NearCacheMode.DISABLED;
        String configurationName = this.configuration.getConfigurationName();
        String templateName = (configurationName != null) ? configurationName : DefaultTemplate.DIST_SYNC.getTemplateName();
        SessionAttributePersistenceStrategy strategy = this.getAttributePersistenceStrategy();
        this.cacheConfigurator = new RemoteCacheServiceConfigurator<>(this.getServiceName().append("cache"), this.configuration.getContainerName(), this.getDeploymentName(), new Consumer<RemoteCacheConfigurationBuilder>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).nearCacheMode(mode).templateName(templateName).transactionMode(TransactionMode.NONE);
                if (mode.invalidated()) {
                    builder.nearCacheFactory(new SessionManagerNearCacheFactory(maxActiveSessions, strategy));
                }
            }
        }).configure(support);
        this.cache = new ServiceSupplierDependency<>(this.cacheConfigurator.getServiceName());
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.cacheConfigurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionManagerFactory<SC, LC, TransactionBatch>> factory = this.cache.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, Function.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SessionManagerFactory<SC, LC, TransactionBatch> get() {
        return new HotRodSessionManagerFactory<>(this);
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.factoryConfiguration.getAttributePersistenceStrategy();
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.factoryConfiguration.getMarshaller();
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.factoryConfiguration.getMaxActiveSessions();
    }

    @Override
    public LocalContextFactory<LC> getLocalContextFactory() {
        return this.factoryConfiguration.getLocalContextFactory();
    }

    @Override
    public String getServerName() {
        return this.factoryConfiguration.getServerName();
    }

    @Override
    public String getDeploymentName() {
        return this.factoryConfiguration.getDeploymentName();
    }

    @Override
    public Immutability getImmutability() {
        return this.factoryConfiguration.getImmutability();
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache() {
        return this.cache.get();
    }

    @Override
    public SpecificationProvider<S, SC, AL> getSpecificationProvider() {
        return this.factoryConfiguration.getSpecificationProvider();
    }

    @Override
    public int getExpirationThreadPoolSize() {
        return this.configuration.getExpirationThreadPoolSize();
    }
}
