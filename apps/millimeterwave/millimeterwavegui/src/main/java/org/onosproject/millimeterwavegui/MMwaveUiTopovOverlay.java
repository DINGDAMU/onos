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
package org.onosproject.millimeterwavegui;

import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import org.onosproject.ui.GlyphConstants;
import org.onosproject.psuccess.Psuccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.ui.topo.TopoConstants.Properties.FLOWS;
import static org.onosproject.ui.topo.TopoConstants.Properties.INTENTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.LATITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.LONGITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.TOPOLOGY_SSCS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TUNNELS;
import static org.onosproject.ui.topo.TopoConstants.Properties.VERSION;

/**
 * Our topology overlay.
 */
public class MMwaveUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in mmwaveTopov.js
    private static final String OVERLAY_ID = "mm-wave overlay";

    private static final String MY_TITLE = "Millimeter wave app";
    private static final String MMWAVE_LINKS = "MM-wave links";
    private static final String ETHERNET_LINKS = "Ethernet links";
    private static final String MAX_PATHS = "MAX paths";
    private static final String PACKET_LOSS_CONSTRAINT = "Packet loss constraint";
    private static final double DEFAULT_PACKET_LOSS_CONSTRAINT = 0.1;
    private static final int DEFAULT_MAX_PATHS = 5;




    private LinkService linkService;
    private HostService hostService;


    private static final ButtonId FOO_BUTTON = new ButtonId("foo");
    private static final ButtonId BAR_BUTTON = new ButtonId("bar");

    private int mmwavelinknum;
    private int ethernetlinknum;
    private int maxpaths;
    private double packetlossconstraint;



    public MMwaveUiTopovOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifySummary(PropertyPanel pp) {
        linkService = get(LinkService.class);
        Set<Link> links = new HashSet<>();
            for (Link link : linkService.getActiveLinks()) {
                links.add(link);
            }
            ethernetlinknum = links.size();
            mmwavelinknum = 0;
        for (Link link : links) {
            if (link.annotations().value("length") != null) {
                mmwavelinknum++;
                ethernetlinknum--;
            }
            }
        pp.title(MY_TITLE)
                .typeId(GlyphConstants.CROWN)
                .removeProps(
                        TOPOLOGY_SSCS,
                        INTENTS,
                        TUNNELS,
                        FLOWS,
                        VERSION
                )
                .addProp(MMWAVE_LINKS, mmwavelinknum)
                .addProp(ETHERNET_LINKS, ethernetlinknum);
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.removeProps(LATITUDE, LONGITUDE);

        pp.addButton(FOO_BUTTON)
                .addButton(BAR_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);
    }


    @Override
    public Map<String, String> additionalLinkData(LinkEvent event) {
        Map<String, String> additional = new HashMap<>();
        Link link = event.subject();
        if (link.annotations().value("length") != null) {
            additional.put("Length", link.annotations().value("length") + "m");
            String len = link.annotations().value("length");
            Double lend = Double.parseDouble(len);
            int ploss = (int) ((1 - Psuccess.getPs(lend)) * 100);
            additional.put("Packet_loss", String.valueOf(ploss) + "%");
        } else {
            additional.put("Length", "default");
            additional.put("Packet_loss", "0%");
        }
        if (link.annotations().value("technology") != null) {
            additional.put("Technology", link.annotations().value("technology"));
        } else {
            additional.put("Technology", "Ethernet");
        }

        if (link.annotations().value("capacity") != null) {
            additional.put("Capacity", link.annotations().value("capacity"));
        } else {
            additional.put("Capacity", "default");
        }
        return additional;

    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {


    }








}
