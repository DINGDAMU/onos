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
package org.onosproject.millimeterwaveport;

import com.google.common.base.Preconditions;
import org.apache.felix.scr.annotations.Activate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceListener;
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
public class MMwavePortProvider extends AbstractProvider
        implements DeviceProvider {


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;


    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String APP_NAME = "org.onosproject.millimeterwaveport";
    private static final String TECHNOLOGY = "technology";

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/portprovider", "port-installer-%d", log));
    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, MMwavePortProviderConfig>(APP_SUBJECT_FACTORY,
                                                                       MMwavePortProviderConfig.class,
                                                                       "ports",
                                                                       true) {
                @Override
                public MMwavePortProviderConfig createConfig() {
                    return new MMwavePortProviderConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    protected DeviceProviderService deviceProviderService;




    private ApplicationId appId;
    private InternalDeviceListener portListener = new InternalDeviceListener();


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        deviceProviderService = providerRegistry.register(this);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        deviceService.addListener(portListener);
        executor.execute(MMwavePortProvider.this::connectComponents);



        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        deviceService.removeListener(portListener);
        providerRegistry.unregister(this);
        deviceProviderService = null;
        executor.shutdown();
        log.info("Stopped");
    }
    //In our case, we need to use openflow,should not be duplicated!
    public MMwavePortProvider() {
        super(new ProviderId("openflow", "org.onosproject.provider.openflow"));
    }


    @Override
    public void triggerProbe(DeviceId deviceId) {

    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole mastershipRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return false;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean b) {

    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(MMwavePortProvider.this::connectComponents);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(MMwavePortProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    private void connectComponents() {
        MMwavePortProviderConfig cfg = cfgService.getConfig(appId, MMwavePortProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getPortAttributes().forEach((MMwavePortProviderConfig.PortAttributes portAttributes) -> {

                    //configuration objects
                    String tecnology = portAttributes.getTechnology();
                    Preconditions.checkNotNull(tecnology, "The technology wave is null!");

                    String deviceID = portAttributes.getDeviceID();
                    Preconditions.checkNotNull(deviceID, "The  deviceID is null!");

                    int portnumber = portAttributes.getPortnumber();
                    Preconditions.checkNotNull(portnumber, "The portnumber is null!");

                    Boolean isEnabled = portAttributes.getIsEnabled();
                    Preconditions.checkNotNull(isEnabled, "The  port state is null!");

                    Device device = deviceService.getDevice(DeviceId.deviceId(deviceID));
                    PortNumber portNumber = PortNumber.portNumber(portnumber);

                    //configuration object
                    SparseAnnotations annotationsPort = DefaultAnnotations.builder()
                            .set(TECHNOLOGY, String.valueOf(tecnology))
                            .build();
                    SparseAnnotations annotationsDevice = DefaultAnnotations.builder()
                            .build();


                    //Must discover devices and get device description before ports' annotation
                    if (device != null) {
                        DeviceDescription description = new DefaultDeviceDescription(device.id().uri(), device.type(),
                                device.manufacturer(), device.hwVersion(),
                                device.swVersion(), device.serialNumber(),
                                device.chassisId(), false, annotationsDevice);
                        deviceProviderService.deviceConnected(device.id(), description);

                    }

                    //if ports are not discovered, retry the discovery
                    if (deviceService.getPorts(device.id()).isEmpty()) {
                        discoverPorts(device.id());
                    }

                    PortDescription description = new DefaultPortDescription(portNumber, isEnabled, annotationsPort);
                    deviceProviderService.portStatusChanged(device.id(), description);


                });
            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }

    /**
     * Listener for core port events.
     */
    private class InternalDeviceListener implements DeviceListener {


        @Override
        public void event(DeviceEvent deviceEvent) {
            if ((deviceEvent.type() == DeviceEvent.Type.DEVICE_ADDED)) {
                executor.execute(MMwavePortProvider.this::connectComponents);


            } else if ((deviceEvent.type() == DeviceEvent.Type.DEVICE_REMOVED)) {
                log.debug("removing devices {}", deviceEvent.subject().id());
                deviceService.getDevice(deviceEvent.subject().id()).annotations().keys();


            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            if (mastershipService.getMasterFor(event.subject().id()) == null) {
                return true;
            }
            return event.subject().annotations().value(AnnotationKeys.PROTOCOL)
                    .equals("openflow".toUpperCase()) &&
                    mastershipService.isLocalMaster(event.subject().id());
        }
    }


    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        //TODO remove when PortDiscovery is removed from master
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            deviceProviderService.updatePorts(deviceId,
                                              portConfig.getPorts());
        } else if (device.is(DeviceDescriptionDiscovery.class)) {
            DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                    device.as(DeviceDescriptionDiscovery.class);
            deviceProviderService.updatePorts(deviceId,
                                              deviceDescriptionDiscovery.discoverPortDetails());
        } else {
            log.warn("No portGetter behaviour for device {}", deviceId);
        }
    }

}
