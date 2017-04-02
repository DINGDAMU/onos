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

import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;

/**
 * Created by dingdamu on 17/1/4.
 */
@Command(scope = "onos", name = "showcomponents",
        description = "Lists all components in the current topology")
public class ShowComponentsCommand extends AbstractShellCommand {

    private static final String FMT_HOST =
            "id=%s, mac=%s, location=%s/%s, vlan=%s, ip(s)=%s%s, configured=%s";
    private static final String FMT_DEVICE =
            "id=%s, available=%s, role=%s, type=%s, mfr=%s, hw=%s, sw=%s, serial=%s, driver=%s%s";
    private static final String FMT_LINK = "src=%s/%s, dst=%s/%s, type=%s, state=%s%s, expected=%s";


    @Option(name = "-d", aliases = "--devices",
            description = "show only devices", required = false,
            multiValued = false)
    private boolean showdevice = false;

    @Option(name = "-h", aliases = "--hosts",
            description = "show only hosts", required = false,
            multiValued = false)
    private boolean showhost = false;

    @Option(name = "-l", aliases = "--links",
            description = "show only links", required = false,
            multiValued = false)
    private boolean showlink = false;


    protected DeviceService deviceService;

    protected HostService hostService;

    protected LinkService linkService;


    /**
     * Initializes the context for all cluster commands.
     */
    protected void init() {
        deviceService = get(DeviceService.class);
        hostService = get(HostService.class);
        linkService = get(LinkService.class);

    }

    @Override
    protected void execute() {
        init();
        if (showdevice) {
            for (Device device : deviceService.getDevices()) {
                printDevice(deviceService, device);
            }
        } else if (showhost) {
            for (Host host : hostService.getHosts()) {
                printHost(host);
            }
        } else if (showlink) {
            for (Link link : linkService.getLinks()) {
                print(linkString(link));
            }
        } else {
            //print all components
            print("Devices:");
            for (Device device : deviceService.getDevices()) {
                printDevice(deviceService, device);
            }

            print("Hosts:");
            for (Host host : hostService.getHosts()) {
                printHost(host);
            }

            print("Links:");
            for (Link link : linkService.getLinks()) {
                print(linkString(link));
            }


        }

    }

    /**
     * Prints information about a host.
     *
     * @param host end-station host
     */
    protected void printHost(Host host) {
        print(FMT_HOST, host.id(), host.mac(),
              host.location().deviceId(), host.location().port(),
              host.vlan(), host.ipAddresses(), annotations(host.annotations()),
              host.configured());

    }

    /**
     * Returns a formatted string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String linkString(Link link) {
        return String.format(FMT_LINK, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port(),
                             link.type(), link.state(),
                             annotations(link.annotations()),
                             link.isExpected());
    }

    /**
     * Prints information about the specified device.
     *
     * @param service device service
     * @param device        infrastructure device
     */
    protected void printDevice(DeviceService service, Device device) {
        if (device != null) {
            String driver = get(DriverService.class).getDriver(device.id()).name();


            print(FMT_DEVICE, device.id(), service.isAvailable(device.id()),
                  service.getRole(device.id()), device.type(),
                  device.manufacturer(), device.hwVersion(), device.swVersion(),
                  device.serialNumber(), driver,
                  annotations(device.annotations(), ImmutableSet.of(AnnotationKeys.DRIVER)));
        }
    }
}


