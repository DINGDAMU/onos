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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dingdamu on 2017/3/20.
 */
@Beta
public final class MMWaveIntent extends ConnectivityIntent {
    static final LinkTypeConstraint NOT_OPTICAL = new LinkTypeConstraint(false, Link.Type.OPTICAL);

    private final HostId one;
    private final HostId two;

    /**
     * Returns a new mm-wave intent builder.
     *
     * @return mm-wave intent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of a host to host intent.
     */
    public static final class Builder extends ConnectivityIntent.Builder {
        HostId one;
        HostId two;

        private Builder() {
            // Hide constructor
        }

        @Override
        public Builder appId(ApplicationId appId) {
            return (Builder) super.appId(appId);
        }

        @Override
        public Builder key(Key key) {
            return (Builder) super.key(key);
        }

        @Override
        public Builder selector(TrafficSelector selector) {
            return (Builder) super.selector(selector);
        }

        @Override
        public Builder treatment(TrafficTreatment treatment) {
            return (Builder) super.treatment(treatment);
        }

        @Override
        public Builder constraints(List<Constraint> constraints) {
            return (Builder) super.constraints(constraints);
        }

        @Override
        public Builder priority(int priority) {
            return (Builder) super.priority(priority);
        }

        /**
         * Sets the first host of the intent that will be built.
         *
         * @param hostone first host
         * @return this builder
         */
        public Builder one(HostId hostone) {
            this.one = hostone;
            return this;
        }

        /**
         * Sets the second host of the intent that will be built.
         *
         * @param hosttwo second host
         * @return this builder
         */
        public Builder two(HostId hosttwo) {
            this.two = hosttwo;
            return this;
        }



        /**
         * Builds a host to host intent from the accumulated parameters.
         *
         * @return point to point intent
         */
        public MMWaveIntent build() {

            List<Constraint> theConstraints = constraints;
            // If not-OPTICAL constraint hasn't been specified, add them
            if (!constraints.contains(NOT_OPTICAL)) {
                theConstraints = ImmutableList.<Constraint>builder()
                        .add(NOT_OPTICAL)
                        .addAll(constraints)
                        .build();
            }

            return new MMWaveIntent(
                    appId,
                    key,
                    one,
                    two,
                    selector,
                    treatment,
                    theConstraints,
                    priority,
                    resourceGroup
            );
        }
    }


    /**
     * Creates a new host-to-host intent with the supplied host pair.
     *
     * @param appId       application identifier
     * @param key       intent key
     * @param one         first host
     * @param two         second host
     * @param selector    action
     * @param treatment   ingress port
     * @param constraints optional prioritized list of path selection constraints
     * @param priority    priority to use for flows generated by this intent
     * @param resourceGroup resource group for this intent
     * @throws NullPointerException if {@code one} or {@code two} is null.
     */
    private MMWaveIntent(ApplicationId appId, Key key,
                             HostId one, HostId two,
                             TrafficSelector selector,
                             TrafficTreatment treatment,
                             List<Constraint> constraints,
                             int priority,
                             ResourceGroup resourceGroup) {
        super(appId, key, ImmutableSet.of(one, two), selector, treatment,
                constraints, priority, resourceGroup);

        // TODO: consider whether the case one and two are same is allowed
        this.one = checkNotNull(one);
        this.two = checkNotNull(two);
    }

    /**
     * Returns identifier of the first host.
     *
     * @return first host identifier
     */
    public HostId one() {
        return one;
    }

    /**
     * Returns identifier of the second host.
     *
     * @return second host identifier
     */
    public HostId two() {
        return two;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("constraints", constraints())
                .add("one", one)
                .add("two", two)
                .toString();
    }
}
