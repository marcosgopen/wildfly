/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel.HotelParticipant;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.model.Booking;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Base class for LRA participant integration tests. Provides HTTP helpers
 * for booking, closing/cancelling LRAs, and validating results.
 */
public abstract class AbstractLRAParticipantTestCase {

    private static final String LRA_COORDINATOR_URL_KEY = "lra.coordinator.url";
    private static final String CLOSE_PATH = "/close";
    private static final String CANCEL_PATH = "/cancel";
    private static final Pattern UID_REGEXP_EXTRACT_MATCHER = Pattern.compile(".*/([^/?]+).*");

    protected CloseableHttpClient client;

    /**
     * Returns the base URL of the deployed test application (injected via {@code @ArquillianResource}).
     */
    protected abstract URL getBaseURL();

    /**
     * Returns the full URL of the LRA coordinator endpoint, e.g.
     * {@code http://localhost:8080/lra-coordinator/lra-coordinator}.
     */
    protected abstract String getCoordinatorUrl();

    @Before
    public void before() {
        System.setProperty(LRA_COORDINATOR_URL_KEY, getCoordinatorUrl());
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() throws IOException {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            System.clearProperty(LRA_COORDINATOR_URL_KEY);
        }
    }

    protected URI startBooking() throws Exception {
        Booking b = bookHotel("Paris-hotel");
        return new URI(b.getId());
    }

    protected String getLRAUid(String lraId) {
        return lraId == null ? null : UID_REGEXP_EXTRACT_MATCHER.matcher(lraId).replaceFirst("$1");
    }

    protected void validateBooking(boolean isEntryPresent) throws Exception {
        try (CloseableHttpResponse response = client.execute(new HttpGet(
                uriFrom(getBaseURL().toURI(), HotelParticipant.HOTEL_PARTICIPANT_PATH)))) {
            String result = EntityUtils.toString(response.getEntity());
            if (isEntryPresent) {
                Assert.assertTrue("Booking confirmed", result.contains("CONFIRMED"));
            } else {
                Assert.assertTrue("Booking cancelled", result.contains("CANCELLED"));
            }
        }
    }

    protected Booking bookHotel(String name) throws Exception {
        try (CloseableHttpResponse response = client.execute(new HttpPost(
                new URIBuilder(uriFrom(getBaseURL().toURI(), HotelParticipant.HOTEL_PARTICIPANT_PATH))
                        .addParameter("hotelName", name)
                        .build()))) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("hotel booking problem; response status = " + response.getStatusLine().getStatusCode());
            } else if (response.getEntity() != null) {
                String result = EntityUtils.toString(response.getEntity());
                ObjectMapper obj = new ObjectMapper();
                return obj.readValue(result, Booking.class);
            } else {
                throw new Exception("hotel booking problem; no entity");
            }
        }
    }

    protected void closeLRA(String lraId) throws Exception {
        endLRA(lraId, true);
    }

    protected void cancelLRA(String lraId) throws Exception {
        endLRA(lraId, false);
    }

    private void endLRA(String lraId, boolean close) throws Exception {
        URI coordinatorURI = new URI(System.getProperty(LRA_COORDINATOR_URL_KEY));
        HttpPut request = new HttpPut(uriFrom(coordinatorURI, close ? lraId.concat(CLOSE_PATH) : lraId.concat(CANCEL_PATH)));
        request.setHeader("Narayana-LRA-API-version", "1.0");
        request.setEntity(new StringEntity(""));
        client.execute(request);
    }

    protected static URI uriFrom(URI baseURI, String... paths) {
        StringBuilder sb = new StringBuilder(baseURI.toString());
        Arrays.stream(paths).forEach(s -> sb.append(s.startsWith("/") ? s : "/" + s));
        return URI.create(sb.toString());
    }
}
