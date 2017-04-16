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
import org.apache.karaf.shell.commands.Option;
import org.onlab.graph.Path;
import org.onlab.graph.KShortestPathsSearch;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.LinksListCommand;
import org.onosproject.common.DefaultTopologyGraph;
import org.onosproject.net.Host;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.HostId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.DefaultTopologyVertex;
//import org.onlab.graph.EdgeWeigher;
import org.onosproject.net.topology.LinkWeigher;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.cli.net.LinksListCommand.compactLinkString;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;



@Command(scope = "onos", name = "mmwave-hosts-paths",
        description = "calculate shortest path between hosts with own customized link weight")
public class MMWaveHostsPathsCommand extends AbstractShellCommand {
    private static final String SEP = "==>";
    private static final int ETHERNET_DEFAULT_COST = 101;
    private static final double PACKET_LOSS_CONSTRAINT = 0.2;
    /**
     * Default weight based on ETHERNET default weight.
     */
    public static final ScalarWeight ETHERNET_DEFAULT_WEIGHT =
            new ScalarWeight(ETHERNET_DEFAULT_COST);
    private static final KShortestPathsSearch<TopologyVertex, TopologyEdge> KSP =
            new KShortestPathsSearch<>();
    private final ProviderId providerId = new ProviderId("FNL", "Ding");

    @Argument(index = 0, name = "src", description = "Source device ID",
            required = true, multiValued = false)
    String srcArg = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
            required = true, multiValued = false)
    String dstArg = null;

    @Option(name = "-f", aliases = "--filter",
            description = "filter with packet loss", required = false,
            multiValued = false)
    boolean filter = false;

    protected PathService pathService;
    protected HostService hostService;
    protected DeviceService deviceService;
    protected LinkService linkService;
    protected TopologyGraph graph;
    protected double totalPs = 1;
    protected double totalLoss;


    //In our case we need to use pathService (ElementID is more comfortable than DeviceID in Topology.getPath() case)
    protected void init() {
        pathService = get(PathService.class);
        hostService = get(HostService.class);
        deviceService = get(DeviceService.class);
        linkService = get(LinkService.class);
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
        long nanos = System.nanoTime();
        long millis = System.currentTimeMillis();
        GraphDescription desc = new DefaultGraphDescription(nanos, millis,
                deviceService.getAvailableDevices(),
                linkService.getActiveLinks());
        graph = new DefaultTopologyGraph(desc.vertexes(), desc.edges());
        DefaultTopologyVertex srcV = new DefaultTopologyVertex(srcLoc);
        DefaultTopologyVertex dstV = new DefaultTopologyVertex(dstLoc);
//        MMwaveEdgeWeight w = new MMwaveEdgeWeight();
        MMwaveLinkWeight w = new MMwaveLinkWeight();
        Set<Path<TopologyVertex, TopologyEdge>> paths = KSP.search(graph, srcV, dstV, w, 8).paths();
        if (paths.isEmpty()) {
            print("The path is empty!");
            return;
        }
        Link srclink = getEdgeLink(srchost, true);
        Link dstlink = getEdgeLink(dsthost, false);
        Set<Path<TopologyVertex, TopologyEdge>> result = new HashSet<>();
        Iterator<Path<TopologyVertex, TopologyEdge>> it = paths.iterator();
        while (it.hasNext()) {
            Path potentialpath = it.next();
            List<Link> pathlinks = getLinks(potentialpath);
            for (Link pathlink : pathlinks) {
                String v = pathlink.annotations().value("length");
                if (v != null) {
                    double ps = Psuccess.getPs(Double.parseDouble(v));
                    totalPs = totalPs * ps;
                }
            }
            totalLoss = 1 - totalPs;
            if (totalLoss > PACKET_LOSS_CONSTRAINT) {
                totalPs = 1;
            } else {
                result.add(potentialpath);
                break;
            }
        }
            if (outputJson()) {
                print("%s", json(this, result));
            } else if (filter) {
                for (Path path : result) {
                    String loss = String.valueOf((int) (totalLoss * 100)) + "%";
                    String constraint = String.valueOf(PACKET_LOSS_CONSTRAINT * 100) + "%";
                    print("The total packet loss is %s below %s", loss, constraint);
                    print(pathString(path, srclink, dstlink));
                }
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
    public static JsonNode json(AbstractShellCommand context, Iterable<Path<TopologyVertex, TopologyEdge>> paths) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Path path : paths) {
            Weight cost = path.cost();
            result.add(LinksListCommand.json(context, networkPath(path))
                               .put("cost", ((ScalarWeight) cost).value())
                               .set("links", LinksListCommand.json(context, getLinks(path))));
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
    private  String pathString(Path path, Link srclink, Link dstlink) {
        StringBuilder sb = new StringBuilder();
        sb.append(compactaSrcEdgeLinkString(srclink)).append(SEP);
        List<Link> links = getLinks(path);
        for (Link link : links) {
            sb.append(compactLinkString(link)).append(SEP);
        }
        sb.append(compactaDstEdgeLinkString(dstlink));
        Weight cost = path.cost();
        sb.append("; cost=").append(((ScalarWeight) cost).value());
        return sb.toString();
    }

//    class MMwaveEdgeWeight implements EdgeWeigher<TopologyVertex, TopologyEdge> {
//
//
//
//        @Override
//        public Weight weight(TopologyEdge edge) {
//            //AnnotationKeys
//            //This can help us to define cost function by annotations
//            String v = edge.link().annotations().value("length");
//
//
//            try {
//
//                if (v != null) {
//                    double ps = Psuccess.getPs(Double.parseDouble(v));
//                    return new ScalarWeight(1 + 1 / ps);
//                } else {
//                    return ETHERNET_DEFAULT_WEIGHT;
//                }
//                //total cost = fixed cost + dynamic cost
//                // In Ethernet case, total cost = 100 + 1; (ps = 100%)
//                // In mm-wave case, total cost = 1 + 1/ps;
//            } catch (NumberFormatException e) {
//                return null;
//            }
//        }
//
//        @Override
//        public Weight getInitialWeight() {
//            return ETHERNET_DEFAULT_WEIGHT;
//        }
//
//        @Override
//        public Weight getNonViableWeight() {
//            return ScalarWeight.NON_VIABLE_WEIGHT;
//        }
//    }

    class MMwaveLinkWeight implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge edge) {

            //AnnotationKeys
            //This can help us to define cost function by annotations
            String v = edge.link().annotations().value("length");


            try {

                if (v != null) {
                    double ps = Psuccess.getPs(Double.parseDouble(v));
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
    // Converts graphs edge to links
    private static List<Link> getLinks(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link)
                .collect(Collectors.toList());
        return links;
    }
    // Converts graph path to a network path with the same cost.
    private static org.onosproject.net.Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link)
                .collect(Collectors.toList());
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }
}
