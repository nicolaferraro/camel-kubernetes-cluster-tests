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

import java.net.InetAddress;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kubernetes.ha.KubernetesClusterService;
import org.apache.camel.impl.ha.ClusteredRoutePolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author nicola
 * @since 30/06/2017
 */
@SpringBootApplication
public class App extends RouteBuilder {

    public static void main(String[] args) {
        String testsSystem = System.getenv("TESTS_SERVICE_HOST");
        if (testsSystem == null) {
            System.out.println("Test system not available. Restarting");
            System.exit(1);
        }

        SpringApplication.run(App.class, args);
    }

    public void configure() throws Exception {

        String mode = System.getenv("MODE");
        if (mode == null) {
            mode = "DEFAULT";
        }

        KubernetesClusterService kubernetes;
        if (mode.equals("DEFAULT")) {
            kubernetes = new KubernetesClusterService();
        } else if (mode.equals("RECREATING")) {
            kubernetes = new KubernetesClusterService();
            kubernetes.setWatchRefreshIntervalSeconds(20L);
        } else {
            throw new IllegalStateException("Unsupported mode: " + mode);
        }

        getContext().addService(kubernetes);


        from("timer:messages")
                .routePolicy(ClusteredRoutePolicy.forNamespace(kubernetes, "leader-app"))
                .log("I'm the leader (${header.CamelTimerCounter})")
                .hystrix()
                    .hystrixConfiguration().executionTimeoutInMilliseconds(3000).executionTimeoutEnabled(true).end()
                    .removeHeaders("*")
                    .setHeader(Exchange.HTTP_METHOD, HttpMethods.POST)
                    .transform().constant(InetAddress.getLocalHost().getHostName())
                    .to("http4://tests:8080/camel/leaders")
                    .log(LoggingLevel.INFO, "Current leader successfully sent!")
                .onFallback()
                    .log(LoggingLevel.WARN, "Cannot send the current leader to the collector endpoint")
                .end();

        rest().get("/kill")
                .route()
                .process(x -> Runtime.getRuntime().halt(1));


    }
}
