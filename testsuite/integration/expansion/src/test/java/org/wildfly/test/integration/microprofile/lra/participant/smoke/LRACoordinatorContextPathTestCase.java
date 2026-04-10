/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.smoke;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.lra.EnableLRASetupTaskWithCustomContextPath;

import java.net.URI;
import java.net.URL;

/**
 * Tests that the LRA coordinator works correctly when deployed with {@code context-path="/"}.
 * With this configuration the coordinator endpoint becomes available at
 * {@code /lra-coordinator} instead of the default {@code /lra-coordinator/lra-coordinator}.
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableLRASetupTaskWithCustomContextPath.class)
public class LRACoordinatorContextPathTestCase extends AbstractLRAParticipantTestCase {

    @ArquillianResource
    public URL baseURL;

    @Override
    protected URL getBaseURL() {
        return baseURL;
    }

    @Override
    protected String getCoordinatorUrl() {
        // With context-path="/", the coordinator URL drops one path segment
        return "http://localhost:8080/lra-coordinator";
    }

    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, "lra-context-path-test.war")
            .addPackages(true,
                "org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel",
                "org.wildfly.test.integration.microprofile.lra.participant.smoke.model")
            .addClasses(LRACoordinatorContextPathTestCase.class,
                AbstractLRAParticipantTestCase.class,
                EnableLRASetupTaskWithCustomContextPath.class,
                CLIServerSetupTask.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testCoordinatorAtCustomContextPath() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        closeLRA(id);
        validateBooking(true);
    }

    @Test
    public void testCompensationAtCustomContextPath() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        cancelLRA(id);
        validateBooking(false);
    }
}
