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
package org.onosproject.millimeterwavelink;

import com.google.common.base.Preconditions;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
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
public  class MMwaveLinkLatencyProvider extends AbstractProvider
        implements LinkProvider {


    //SLF4J
    private final Logger log = LoggerFactory.getLogger(getClass());

     //<onos.app.name>
    protected static final String APP_NAME = "org.onosproject.millimeterwavelinklatency";
    //private static final String SCHEME_NAME = "openflow";
    private static final String MMLATENCY = "mmlatency";


    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/linkprovider", "link-installer-%d", log));


    //In our case, we need to use openflow
    public MMwaveLinkLatencyProvider() {
        super(new ProviderId("off", "org.onosproject.provider.openflow"));
    }



    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;







    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, MMwaveLinkProviderConfig>(APP_SUBJECT_FACTORY,
                                                                    MMwaveLinkProviderConfig.class,
                                                                    "linksLatency",
                                                                    true) {
                @Override
                public MMwaveLinkProviderConfig createConfig() {
                    return new MMwaveLinkProviderConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();


    private ApplicationId appId;
    protected LinkProviderService linkProviderService;
    private InternalLinkListener linkListener = new InternalLinkListener();



    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        linkProviderService = providerRegistry.register(this);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        linkService.addListener(linkListener);
        executor.execute(MMwaveLinkLatencyProvider.this::connectComponents);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        linkService.removeListener(linkListener);
        providerRegistry.unregister(this);
        executor.shutdown();
        log.info("Stopped");
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(MMwaveLinkLatencyProvider.this::connectComponents);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(MMwaveLinkProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    private void connectComponents() {
        MMwaveLinkProviderConfig cfg = cfgService.getConfig(appId, MMwaveLinkProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getLinkAttibutes().forEach(linkAttributes -> {

                    String mmlatency = linkAttributes.getMMLatency();
                    Preconditions.checkNotNull(mmlatency, "The latency is null!");



                    String srcArg = linkAttributes.getSrc();
                    Preconditions.checkNotNull(srcArg, "The source connect_point is null!");

                    String dstArg = linkAttributes.getDst();
                    Preconditions.checkNotNull(dstArg, "The destination connect_point is null!");

                    ConnectPoint src = ConnectPoint.deviceConnectPoint(srcArg);
                    ConnectPoint dst = ConnectPoint.deviceConnectPoint(dstArg);
                    //configuration object
                    SparseAnnotations annotations = DefaultAnnotations.builder()
                            .set(MMLATENCY, String.valueOf(mmlatency))
                            .build();

                    Link link = linkService.getLink(src, dst);





                    if (link == null) {
                        log.warn("Link {} has not been added to store, " +
                                         "maybe due to a problem in connectivity", src + "/" + dst);
                    } else {
                        //Bidirectional annotation
                        LinkDescription description = new DefaultLinkDescription(src, dst, link.type(), annotations);
                        LinkDescription description1 = new DefaultLinkDescription(dst, src, link.type(), annotations);
                        linkProviderService.linkDetected(description);
                        linkProviderService.linkDetected(description1);

                    }
                });
            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }



    /**
     * Listener for core link events.
     */
    private class InternalLinkListener implements LinkListener {


        @Override
        public void event(LinkEvent linkEvent) {
            if ((linkEvent.type() == LinkEvent.Type.LINK_ADDED)) {
                executor.execute(MMwaveLinkLatencyProvider.this::connectComponents);


         }
        }


    }
}
