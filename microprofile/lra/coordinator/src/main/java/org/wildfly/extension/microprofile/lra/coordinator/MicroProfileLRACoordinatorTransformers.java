/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class MicroProfileLRACoordinatorTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ModelVersion version1_0_0 = MicroProfileLRACoordinatorSubsystemModel.VERSION_1_0_0.getVersion();

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory
                .createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());

        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), version1_0_0);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, MicroProfileLRACoordinatorSubsystemDefinition.CONTEXT_PATH)
                .addRejectCheck(RejectAttributeChecker.DEFINED, MicroProfileLRACoordinatorSubsystemDefinition.CONTEXT_PATH);

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{version1_0_0});
    }
}
