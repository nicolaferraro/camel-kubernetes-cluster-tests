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

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nicola
 * @since 30/06/2017
 */
@RunWith(Arquillian.class)
public class LeaderKT extends KubernetesTestBase {

    private static final long INITIAL_DELAY = 20000L;
    private static final long KILL_TIME_AVG = 18000L;
    private static final long KILL_TIME_DEV = 5000L;
    private static final long MAX_RUN_TIME = 300000L;
    private static final double FORCEFUL_KILL_PROB = 0.35;


//    @Test
//    public void recreatingSystemTest() throws Exception {
//        setEnv("MODE", "RECREATING");
//        doTest();
//    }

    @Test
    public void defaultSystemTest() throws Exception {
        setEnv("MODE", "DEFAULT");
        doTest();
    }

    public void doTest() throws Exception {
        Thread.sleep(INITIAL_DELAY);
        while (true) {
            long time = System.currentTimeMillis();
            long nextEvent = time + KILL_TIME_AVG - KILL_TIME_DEV + (long) (Math.random() * 2 * KILL_TIME_DEV);
            long deadline = testStart + MAX_RUN_TIME;
            if (nextEvent > deadline) {
                long delay = deadline - time;
                if (delay > 0) {
                    System.out.println("Waiting " + (delay / 1000) + " seconds before shutdown...");
                    Thread.sleep(delay);
                }
                System.out.println("Shutdown...");
                break;
            }

            System.out.println("Killing next pod in " + ((nextEvent - time) / 1000) + " seconds...");

            Thread.sleep(nextEvent - time);
            killPod(Math.random() >= FORCEFUL_KILL_PROB);
        }

        verifyLeaderConsistency();
    }

}
