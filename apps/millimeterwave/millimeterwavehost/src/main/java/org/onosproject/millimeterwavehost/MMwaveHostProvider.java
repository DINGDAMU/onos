/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.base.Preconditions;
import org.apache.felix.scr.annotations.Activate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class MMwaveHostProvider extends AbstractProvider
        implements HostProvider {


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;


    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String APP_NAME = "org.onosproject.millimeterwavehost";

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/hostprovider", "host-installer-%d", log));
    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, MMwaveHostProviderConfig>(APP_SUBJECT_FACTORY,
                                                                       MMwaveHostProviderConfig.class,
                                                                       "hosts",
                                                                       true) {
                @Override
                public MMwaveHostProviderConfig createConfig() {
                    return new MMwaveHostProviderConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    protected HostProviderService hostProviderService;




    private ApplicationId appId;
    private HostListener hostListener = new InternalHostListener();


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        hostProviderService = providerRegistry.register(this);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        hostService.addListener(hostListener);
        executor.execute(MMwaveHostProvider.this::connectComponents);



        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        hostService.removeListener(hostListener);
        providerRegistry.unregister(this);
        hostProviderService = null;
        executor.shutdown();
        log.info("Stopped");
    }
    //In our case, we need to use openflow,should not be duplicated!
    public MMwaveHostProvider() {
        super(new ProviderId("ofhost", "org.onosproject.provider.openflow"));
    }
    @Override
    public void triggerProbe(Host host) {
        //log.info("Triggering probe on device {} ", host);
    }
    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(MMwaveHostProvider.this::connectComponents);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(MMwaveHostProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    private void connectComponents() {
        MMwaveHostProviderConfig cfg = cfgService.getConfig(appId, MMwaveHostProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getGraphAttributes().forEach((MMwaveHostProviderConfig.HostAttributes hostAttributes) -> {

                    //configuration objects
                    String hostArg = hostAttributes.getHostid();
                    Preconditions.checkNotNull(hostArg, "The host id is null!");
                    int maxpaths = hostAttributes.getMaxpaths();
                    Preconditions.checkNotNull(maxpaths, "The maxpath is null!");
                    double packetlossConstraint = hostAttributes.getPacketlossconstraint();
                    Preconditions.checkNotNull(packetlossConstraint, "The  packet loss constraint is null!");


                    //configuration object
                    SparseAnnotations an = DefaultAnnotations.builder()
                            .set(MMwaveHostProviderConfig.MAX_PATHS, String.valueOf(maxpaths))
                            .set(MMwaveHostProviderConfig.PACKET_LOSS_CONSTRAINT, String.valueOf(packetlossConstraint))
                            .build();

                    HostId hostId = HostId.hostId(hostArg);
                    Host host = hostService.getHost(hostId);
                    if (host != null) {
                        MacAddress mac = host.mac();
                        VlanId vlan = host.vlan();
                        HostLocation loc = host.location();
                        HostDescription hostDescription = new DefaultHostDescription(mac, vlan, loc, an);
                        hostProviderService.hostDetected(hostId, hostDescription, false);
                    }
                });
            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }

    /**
     * Listener for core port events.
     */
    private class InternalHostListener implements HostListener {


        @Override
        public void event(HostEvent event) {
            executor.execute(MMwaveHostProvider.this::connectComponents);

        }
    }



}
