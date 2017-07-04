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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.v2_2.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.v2_2.Pod;
import io.fabric8.kubernetes.clnt.v2_2.KubernetesClient;
import io.fabric8.openshift.clnt.v2_2.DefaultOpenShiftClient;
import io.fabric8.openshift.clnt.v2_2.OpenShiftClient;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author nicola
 * @since 04/07/2017
 */
public class KubernetesTestBase {

    protected CamelContext context;

    //@ArquillianResource
    protected OpenShiftClient client;

    @ArquillianResource
    @Named("tests")
    @PortForward
    private URL service;

    protected Long testStart;

    @Before
    public void prepare() throws Exception {
        client = new DefaultOpenShiftClient();
        context = new DefaultCamelContext();

        new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:leaders")
                        .setHeader(Exchange.ACCEPT_CONTENT_TYPE, constant("application/json"))
                        .toF("http4://%s:%s/camel/leaders?bridgeEndpoint=true&connectionClose=true", service.getHost(), service.getPort())
                        .unmarshal().json(JsonLibrary.Jackson, LeaderStore.Leaders.class);

                from("direct:reset")
                        .setHeader(Exchange.ACCEPT_CONTENT_TYPE, constant("text/plain"))
                        .setHeader(Exchange.HTTP_METHOD, HttpMethods.POST)
                        .toF("http4://%s:%s/camel/reset?bridgeEndpoint=true&connectionClose=true", service.getHost(), service.getPort());

                from("direct:killOne")
                        .toF("http4://%s:%s/camel/kill?bridgeEndpoint=true&connectionClose=true", "leader-app-test.127.0.0.1.nip.io", 80);

            }
        }.addRoutesToCamelContext(context);

        context.start();

        resetLeaderStore();

        testStart = System.currentTimeMillis();
    }

    @After
    public void destroy() throws Exception {
        if (context != null) {
            context.stop();
        }
        if (client != null) {
            client.close();
        }
    }

    protected void setEnv(String key, String value) {
        client.deploymentConfigs().withName("leader-app").edit()
                .editOrNewSpec()
                    .editOrNewTemplate()
                        .editOrNewSpec()
                            .editFirstContainer()
                                .addToEnv(new EnvVarBuilder().withName(key).withValue(value).build())
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .done();
    }

    protected void killPod(boolean gracefully) {
        if (gracefully) {
            System.out.println("Doing pod graceful delete");

            List<Pod> pods = client.pods().withLabel("deploymentconfig", "leader-app").list().getItems();
            assertTrue("No pods to kill found", pods.size() > 0);
            int pos = (int) (Math.random() * pods.size());
            String podName = pods.get(pos).getMetadata().getName();
            System.out.println("Bye " + podName + "...");
            client.pods().inNamespace("test").withName(podName).delete();
        } else {
            System.out.println("Killing brutally a pod");
            try {
                context.createProducerTemplate().sendBody("direct:killOne", null);
            } catch (Exception e) {
                System.out.println("Killed! (should not respond)");
            }
        }
    }

    protected void resetLeaderStore() {
        context.createProducerTemplate().sendBody("direct:reset", null);
    }

    protected void verifyLeaderConsistency() {
        // Verification
        long end = System.currentTimeMillis();

        LeaderStore.Leaders leaders = context.createProducerTemplate().requestBody("direct:leaders", null, LeaderStore.Leaders.class);
        List<LeaderStore.Leader> leaderList = new ArrayList<>(leaders.getLeaders());

        long totalDuration = end - testStart;
        int minEvents = (int) (0.20 * (totalDuration / 1000)); // at least one every 5 seconds



        assertTrue("Found " + leaderList.size() + " events, required (minimum) " + minEvents, leaderList.size() > minEvents);

        Set<String> leaderSet = leaderList.stream().map(LeaderStore.Leader::getLeader).collect(Collectors.toSet());

        assertTrue("No leaders collected", leaderSet.size() > 0);
        System.out.println("Number of leaders: " + leaderSet.size());

        for (String leader : leaderSet) {
            long min = leaderList.stream().filter(l -> l.getLeader().equals(leader)).map(LeaderStore.Leader::getTimestamp).min(Comparator.naturalOrder()).get();
            long max = leaderList.stream().filter(l -> l.getLeader().equals(leader)).map(LeaderStore.Leader::getTimestamp).max(Comparator.naturalOrder()).get();

            Set<String> intersectingLeaders = leaderList.stream().filter(l -> l.getTimestamp() >= min && l.getTimestamp() <= max).map(LeaderStore.Leader::getLeader).collect(Collectors.toSet());
            assertEquals("More than one leader present in a period: " + leaderList, 1, intersectingLeaders.size());
            assertEquals(leader, intersectingLeaders.iterator().next());
        }
    }

}
