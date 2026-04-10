/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

public class MicroProfileLRACoordinatorSubsystemConfigTestCase extends AbstractSubsystemTest {

    public MicroProfileLRACoordinatorSubsystemConfigTestCase() {
        super(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new MicroProfileLRACoordinatorExtension());
    }

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);

    /**
     * Tests that the context-path can be configured to "/".
     */
    @Test
    public void testContextPathRoot() throws Exception {
        String xml = "<subsystem xmlns=\"urn:wildfly:microprofile-lra-coordinator:2.0\" context-path=\"/\"/>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.get(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("/", subsystem.get(CommonAttributes.CONTEXT_PATH).asString());
    }

    /**
     * Tests that the context-path can be configured to a custom path.
     */
    @Test
    public void testContextPathCustom() throws Exception {
        String xml = "<subsystem xmlns=\"urn:wildfly:microprofile-lra-coordinator:2.0\" context-path=\"/my-coordinator\"/>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.get(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("/my-coordinator", subsystem.get(CommonAttributes.CONTEXT_PATH).asString());
    }

    /**
     * Tests that an omitted context-path resolves to the default.
     */
    @Test
    public void testContextPathDefault() throws Exception {
        String xml = "<subsystem xmlns=\"urn:wildfly:microprofile-lra-coordinator:2.0\"/>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.get(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);
        Assert.assertEquals(CommonAttributes.DEFAULT_CONTEXT_PATH, subsystem.get(CommonAttributes.CONTEXT_PATH).asString());
    }

    /**
     * Tests that a context-path not starting with '/' is rejected at write time.
     */
    @Test
    public void testContextPathValidationRejectsNoLeadingSlash() throws Exception {
        String xml = "<subsystem xmlns=\"urn:wildfly:microprofile-lra-coordinator:2.0\"/>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode writeOp = SubsystemOperations.createWriteAttributeOperation(
                SUBSYSTEM_ADDRESS.toModelNode(),
                MicroProfileLRACoordinatorSubsystemDefinition.CONTEXT_PATH,
                "no-leading-slash");
        ModelNode result = services.executeOperation(writeOp);
        Assert.assertFalse("Write of context-path without leading '/' should fail",
                SubsystemOperations.isSuccessfulOutcome(result));
        Assert.assertTrue(SubsystemOperations.getFailureDescriptionAsString(result).contains("must start with '/'"));
    }

    /**
     * Tests that a 1.0 schema XML (without context-path) still boots and defaults correctly.
     */
    @Test
    public void testVersion1SchemaBoots() throws Exception {
        String xml = "<subsystem xmlns=\"urn:wildfly:microprofile-lra-coordinator:1.0\"/>";
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.get(SUBSYSTEM, MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME);
        // context-path gets the default value even when parsed from 1.0 schema
        Assert.assertEquals(CommonAttributes.DEFAULT_CONTEXT_PATH, subsystem.get(CommonAttributes.CONTEXT_PATH).asString());
    }
}
