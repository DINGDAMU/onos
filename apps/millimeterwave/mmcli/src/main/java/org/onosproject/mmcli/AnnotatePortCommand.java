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
package org.onosproject.mmcli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;

@Command(scope = "onos", name = "annotate-ports",
        description = "Annotates ports")
public class AnnotatePortCommand extends AbstractShellCommand {

    static final ProviderId PID = new ProviderId("cli", "org.onosproject.cli", true);

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "portnumber", description = "Port number",
            required = true, multiValued = false)
    String portnumber = null;

    @Argument(index = 2, name = "isEnabled", description = "Port active state",
            required = true, multiValued = false)
    boolean isEnabled = false;

    @Argument(index = 3, name = "key", description = "Annotation key",
            required = true, multiValued = false)
    String key = null;

    @Argument(index = 4, name = "value",
            description = "Annotation value (null to remove)",
            required = false, multiValued = false)
    String value = null;


    @Override
    protected void execute() {
        DeviceService service = get(DeviceService.class);
        Device device = service.getDevice(DeviceId.deviceId(uri));

        DeviceProviderRegistry registry = get(DeviceProviderRegistry.class);
        DeviceProvider provider = new AnnotationProvider();

        PortNumber portNumber = PortNumber.portNumber(portnumber);
        try {
            DeviceProviderService providerService = registry.register(provider);

            //discover ports
            if (device.is(PortDiscovery.class)) {
                PortDiscovery portConfig = device.as(PortDiscovery.class);
                providerService.updatePorts(device.id(),
                                            portConfig.getPorts());
            } else if (device.is(DeviceDescriptionDiscovery.class)) {
                DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                        device.as(DeviceDescriptionDiscovery.class);
                providerService.updatePorts(device.id(),
                                            deviceDescriptionDiscovery.discoverPortDetails());
            } else {
                log.warn("No portGetter behaviour for device {}", device.id());
            }
            providerService.portStatusChanged(device.id(), description(portNumber, isEnabled, key, value));
        } finally {
            registry.unregister(provider);
        }
    }

    private PortDescription description(PortNumber portNumber, boolean isenabled, String keyValue, String valueofkey) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (valueofkey != null) {
            builder.set(key, valueofkey);
        } else {
            builder.remove(key);
        }

        // don' forget new method!
        return  new DefaultPortDescription(portNumber, isenabled, builder.build());


    }







    // Token provider entity
    private static final class AnnotationProvider
            extends AbstractProvider implements DeviceProvider {
        private AnnotationProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                    boolean enable) {
        }
    }
}
