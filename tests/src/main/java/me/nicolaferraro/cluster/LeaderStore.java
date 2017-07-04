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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

/**
 * @author nicola
 * @since 30/06/2017
 */
@Component
public class LeaderStore {

    public Queue<Leader> leaders;

    public LeaderStore() {
        reset();
    }

    public void reset() {
        this.leaders = new ConcurrentLinkedQueue<>();
    }

    public void notifyLeader(String leader) {
        this.leaders.add(new Leader(System.currentTimeMillis(), leader));
    }

    public Leaders getLeaderHistory() {
        return new Leaders(new LinkedList<>(leaders));
    }



    public static class Leaders {
        private List<Leader> leaders = new LinkedList<>();

        public Leaders() {
        }

        public Leaders(List<Leader> leaders) {
            this.leaders = leaders;
        }

        public List<Leader> getLeaders() {
            return leaders;
        }

        public void setLeaders(List<Leader> leaders) {
            this.leaders = leaders;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Leaders{");
            sb.append("leaders=").append(leaders);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class Leader {
        private Long timestamp;
        private String leader;

        public Leader(Long timestamp, String leader) {
            this.timestamp = timestamp;
            this.leader = leader;
        }

        public Leader() {
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public String getLeader() {
            return leader;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Leader{");
            sb.append("timestamp=").append(timestamp);
            sb.append(", leader='").append(leader).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
