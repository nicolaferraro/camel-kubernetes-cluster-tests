/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package me.nicolaferraro.cluster;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.jsse.SSLContextClientParameters;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * @author nicola
 * @since 30/06/2017
 */
@RunWith(Arquillian.class)
public class LeaderKT {

    private static final Long MIN_SWITCH_TIME = 20000L;

    @ArquillianResource
    @Named("tests")
    @PortForward
    private URL service;

    private CamelContext context;

    @Before
    public void prepare() throws Exception {
        context = new DefaultCamelContext();

        new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:leaders")
                        .setHeader(Exchange.ACCEPT_CONTENT_TYPE, constant("application/json"))
                        .toF("http4://%s:%s/camel/leaders?bridgeEndpoint=true&connectionClose=true", service.getHost(), service.getPort())
                        .unmarshal().json(JsonLibrary.Jackson, LeaderStore.Leaders.class);

            }
        }.addRoutesToCamelContext(context);

        context.start();
    }

    @After
    public void destroy() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    public void systemTest() throws Exception {

        long start = System.currentTimeMillis();

        // Here you should kill pods randomly...
        Thread.sleep(45000);



        long end = System.currentTimeMillis();

        // Verification
        LeaderStore.Leaders leaders = context.createProducerTemplate().requestBody("direct:leaders", null, LeaderStore.Leaders.class);
        List<LeaderStore.Leader> leaderList = new ArrayList<>(leaders.getLeaders());
        Collections.sort(leaderList, (l1, l2) -> l1.getTimestamp().compareTo(l2.getTimestamp()));

        long totalDuration = end - start;
        int minEvents = (int)(0.5 * (totalDuration/1000)); // at least one every 2 seconds

        assertTrue("Found " + leaderList.size() + " events, required (minimum) " + minEvents, leaderList.size() > minEvents);

        for (int i=0; i<leaderList.size() - 1; i++) {
            LeaderStore.Leader prev = leaderList.get(i);
            LeaderStore.Leader curr = leaderList.get(i + 1);

            if (curr.getLeader().equals(prev.getLeader())) {
                continue;
            }

            long delay = curr.getTimestamp() - prev.getTimestamp();
            assertTrue("Two leaders may be concurrently active (" + curr.getLeader() + " and " + prev.getLeader() + "). Delay: " + delay, delay > MIN_SWITCH_TIME);
        }

    }

}
