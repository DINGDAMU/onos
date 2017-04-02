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
package org.onosproject.millimeterwavelink;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Created by dingdamu on 17/1/11.
 */
public class MMwaveLinkProviderConfig extends Config<ApplicationId> {
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String LENGTH = "length";
    private static final String CAPACITY = "capacity";
    private static final String TECHNOLOGY = "technology";
    private static final String SRC = "src";
    private static final String DST = "dst";


    public Set<LinkAttributes> getLinkAttibutes() throws ConfigException {
        Set<LinkAttributes> linkAttributes = Sets.newHashSet();

        try {
            for (JsonNode node : array) {
                String src = node.path(SRC).asText();
                String dst = node.path(DST).asText();
                String length = node.path(LENGTH).asText();
                String capacity = node.path(CAPACITY).asText();
                String technology = node.path(TECHNOLOGY).asText();
                linkAttributes.add(new LinkAttributes(length, capacity, technology, src, dst));


            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return linkAttributes;
    }


    public class LinkAttributes {
        private final String length;
        private final String capacity;
        private final String technology;
        private final String src;
        private final String dst;


        public LinkAttributes(String length, String capacity, String technology, String src, String dst) {
            this.length = length;
            this.capacity = capacity;
            this.technology = technology;
            this.src = src;
            this.dst = dst;

        }


        public String getLength() {
            return length;
        }

        public String getCapacity() {
            return capacity;
        }

        public String getTechnology() {
            return technology;
        }

        public String getSrc() {
            return src;
        }

        public String getDst() {
            return dst;
        }
    }
}
