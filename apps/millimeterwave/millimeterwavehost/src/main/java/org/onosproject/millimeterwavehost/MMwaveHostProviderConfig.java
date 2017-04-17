/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.millimeterwavehost;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

import java.util.Set;


public class MMwaveHostProviderConfig extends Config<ApplicationId> {
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    public static final String MAX_PATHS = "maxpaths";
    public static final String PACKET_LOSS_CONSTRAINT = "packetlossconstraint";
    public static final String HOST_ID = "hostid";





    public Set<HostAttributes> getGraphAttributes() throws ConfigException {
        Set<HostAttributes> hostAttributes = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String hostid = node.path(HOST_ID).asText();
                int maxpaths = node.path(MAX_PATHS).asInt();
                double packetconstraint = node.path(PACKET_LOSS_CONSTRAINT).asDouble();
                hostAttributes.add(new HostAttributes(maxpaths, packetconstraint, hostid));

            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return hostAttributes;
    }
    public class HostAttributes {
        private final String hostid;
        private final int maxpaths;
        private final double packetlossconstraint;

        public HostAttributes(int maxpaths, double packetlossconstraint, String hostid) {
            this.maxpaths = maxpaths;
            this.packetlossconstraint = packetlossconstraint;
            this.hostid = hostid;
        }
        public int getMaxpaths() {
            return maxpaths;
        }
        public double getPacketlossconstraint() {
            return packetlossconstraint;
        }
        public String getHostid() { return hostid; }
    }

}
