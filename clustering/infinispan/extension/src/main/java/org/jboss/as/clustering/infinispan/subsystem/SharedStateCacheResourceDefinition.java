/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.PathElement;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedStateCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(PartitionHandlingResourceDefinition.PATH, StateTransferResourceDefinition.PATH, BackupsResourceDefinition.PATH);

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addRequiredChildren(REQUIRED_CHILDREN);
        }
    }

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    SharedStateCacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ClusteredCacheServiceHandler handler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, new ResourceDescriptorConfigurator(configurator), handler, executors);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PartitionHandlingResourceDefinition().register(registration);
        new StateTransferResourceDefinition().register(registration);
        new BackupsResourceDefinition(this.executors).register(registration);

        return registration;
    }
}
