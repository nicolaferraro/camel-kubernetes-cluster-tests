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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author nicola
 * @since 30/06/2017
 */
@SpringBootApplication
public class TestApp extends RouteBuilder {

    public static void main(String[] args) {
        SpringApplication.run(TestApp.class, args);
    }


    @Override
    public void configure() throws Exception {

        rest().post("/leaders")
                .consumes("text/plain")
                .produces("text/plain")
                .route()
                .log("Received new leader ${body}")
                .bean("leaderStore", "notifyLeader");

        rest().get("/leaders")
                .produces("application/json")
                .route()
                .bean("leaderStore", "getLeaderHistory")
                .marshal().json(JsonLibrary.Jackson);

    }
}
