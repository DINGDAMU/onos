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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.LinksListCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Host;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;


import java.util.Set;

import static org.onosproject.cli.net.LinksListCommand.compactLinkString;


@Command(scope = "onos", name = "mmwave-hosts-paths",
        description = "calculate shortest path between hosts with own customized link weight")
public class MMWaveHostsPathsCommand extends AbstractShellCommand {
    private static final String SEP = "==>";
    private static final int ETHERNET_DEFAULT_COST = 101;
    /**
     * Default weight based on ETHERNET default weight.
     */
    public static final ScalarWeight ETHERNET_DEFAULT_WEIGHT =
            new ScalarWeight(ETHERNET_DEFAULT_COST);
    private final ProviderId providerId = new ProviderId("FNL", "Ding");

    @Argument(index = 0, name = "src", description = "Source device ID",
            required = true, multiValued = false)
    String srcArg = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
            required = true, multiValued = false)
    String dstArg = null;


    protected PathService pathService;
    protected HostService hostService;
    //In our case we need to use pathService (ElementID is more comfortable than DeviceID in Topology.getPath() case)
    protected void init() {
        pathService = get(PathService.class);
        hostService = get(HostService.class);
    }
    @Override
    protected void execute() {

        init();
        HostId src = HostId.hostId(srcArg);
        HostId dst = HostId.hostId(dstArg);
        Host srchost = hostService.getHost(src);
        Host dsthost = hostService.getHost(dst);
        DeviceId srcLoc = srchost.location().deviceId();
        DeviceId dstLoc = dsthost.location().deviceId();
        Set<Path> paths = pathService.getPaths(srcLoc, dstLoc, new MMwaveLinkWeight());
        if (paths.isEmpty()) {
            print("The path is empty!");
            return;
        }

        Link srclink = getEdgeLink(srchost, true);
        Link dstlink = getEdgeLink(dsthost, false);


        if (outputJson()) {
            print("%s", json(this, paths));
        } else {
            for (Path path : paths) {
                print(pathString(path, srclink, dstlink));
            }
        }
    }
    /**
     * Produces a JSON array containing the specified paths.
     *
     * @param context context to use for looking up codecs
     * @param paths collection of paths
     * @return JSON array
     */
    public static JsonNode json(AbstractShellCommand context, Iterable<Path> paths) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Path path : paths) {
            Weight cost = path.weight();
            result.add(LinksListCommand.json(context, path)
                               .put("cost", ((ScalarWeight) cost).value())
                               .set("links", LinksListCommand.json(context, path.links())));
        }
        return result;
    }

    /**
     * Produces a formatted string representing the specified path.
     *
     * @param path network path
     * @param srclink source host link
     * @param dstlink dst host link
     * @return formatted path string
     */
    protected String pathString(Path path, Link srclink, Link dstlink) {
        StringBuilder sb = new StringBuilder();
        sb.append(compactaSrcEdgeLinkString(srclink)).append(SEP);
        for (Link link : path.links()) {
            sb.append(compactLinkString(link)).append(SEP);
        }
        sb.append(compactaDstEdgeLinkString(dstlink));
        sb.delete(sb.lastIndexOf(SEP), sb.length());
        Weight cost = path.weight();
        sb.append("; cost=").append(((ScalarWeight) cost).value());
        return sb.toString();
    }

    class MMwaveLinkWeight implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge edge) {

            //AnnotationKeys
            //This can help us to define cost function by annotations
            String v = edge.link().annotations().value("length");


            try {

                if (v != null) {
                    Psuccess psuccess = new Psuccess();
                    double ps = psuccess.getPs(Double.parseDouble(v));
                    return new ScalarWeight(1 + 1 / ps);
                } else {
                    return ETHERNET_DEFAULT_WEIGHT;
                }
                //total cost = fixed cost + dynamic cost
                // In Ethernet case, total cost = 100 + 1; (ps = 100%)
                // In mm-wave case, total cost = 1 + 1/ps;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public Weight getInitialWeight() {
            return ETHERNET_DEFAULT_WEIGHT;
        }

        /**
         * Weight value for null path (without links).
         */
        @Override
        public Weight getNonViableWeight() {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }
    }


    /**
     * Generate EdgeLink which is between Host and Device.
     *
     *
     * @param host
     * @param isIngress whether it is Ingress to Device or not.
     * @return
     */
    private EdgeLink getEdgeLink(Host host, boolean isIngress) {
        return new DefaultEdgeLink(providerId, new ConnectPoint(host.id(), PortNumber.portNumber(0)),
                host.location(), isIngress);
    }

    public static String compactaSrcEdgeLinkString(Link srclink) {
        HostId h1 = srclink.src().hostId();
        DeviceId d1 = srclink.dst().deviceId();
        return String.format("%s/%s-%s/%s", new Object[]{h1, 0, d1, srclink.dst().port()});
    }
    public static String compactaDstEdgeLinkString(Link dstlink) {
        DeviceId d1 = dstlink.src().deviceId();
        HostId h1 = dstlink.dst().hostId();
        return String.format("%s/%s-%s/%s", new Object[]{d1, dstlink.src().port(), h1, 0});
    }
}
