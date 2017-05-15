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
import org.onlab.graph.KShortestPathsSearch;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.LinksListCommand;
import org.onosproject.common.DefaultTopology;
import org.onosproject.net.Host;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.HostId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.psuccess.Psuccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.cli.net.LinksListCommand.compactLinkString;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;


@Command(scope = "onos", name = "mmwave-hosts-paths",
        description = "calculate shortest path between hosts with own customized link weight")
public class MMWaveHostsPathsCommand extends AbstractShellCommand {
    private static final String SEP = "==>";
    private static final double ETHERNET_DEFAULT_COST = 101.0;
    private static final double DEFAULT_HOP_COST = 1.0;

    private static final double INITIAL_COST = 0.0;
    private static final double DEFAULT_PACKET_LOSS_CONSTRAINT = 1.0;
    private static final double DEFAULT_BANDWIDTH_CONSTRAINT = 0.0;
    private static final int DEFAULT_MAX_PATHS = 10;
    private static final int INFINITY = 99999;



    /**
     * Default weight based on ETHERNET default weight.
     */
    public static final ScalarWeight ETHERNET_DEFAULT_WEIGHT =
            new ScalarWeight(ETHERNET_DEFAULT_COST);
    public static final ScalarWeight INITIAL_WEIGHT =
            new ScalarWeight(INITIAL_COST);
    public static final ScalarWeight HOP_DEFAULT_WEIGHT =
            new ScalarWeight(DEFAULT_HOP_COST);
    private static final KShortestPathsSearch<TopologyVertex, TopologyEdge> KSP =
            new KShortestPathsSearch<>();
    private final ProviderId providerId = new ProviderId("FNL", "Ding");

    @Argument(index = 0, name = "src", description = "Source device ID",
            required = true, multiValued = false)
    String srcArg = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
            required = true, multiValued = false)
    String dstArg = null;

    @Option(name = "-mp", aliases = "--maxpath",
            description = "max path in ksp algorithm", required = false,
            multiValued = false)
    String k = null;

    @Option(name = "-pl", aliases = "--packetlossconstraint",
            description = "packet loss constraint", required = false,
            multiValued = false)
    String plconstraint = null;

    @Option(name = "-lat", aliases = "--latency",
            description = "Latency constraint", required = false,
            multiValued = false)
    String latconstraint = null;

    @Option(name = "-band", aliases = "--bandwidth",
            description = "Bandwidth constraint", required = false,
            multiValued = false)
    String bwconstraint = null;

    @Option(name = "-mm", aliases = "--millimeterwave",
            description = "Use the millimeter wave link weight", required = false,
            multiValued = false)
    boolean mmwave = false;


    boolean filter = false;

    protected PathService pathService;
    protected HostService hostService;
    protected TopologyService topologyService;
    protected double totalPs = 1.0;
    protected double totalLatency = 0.0;
    protected double totalLoss;
    protected double bandwidth;
    protected int maxpaths = DEFAULT_MAX_PATHS;
    protected double packetlossConstraint = DEFAULT_PACKET_LOSS_CONSTRAINT;
    protected double latencyConstraint = INFINITY;
    protected double bandwidthConstraint = DEFAULT_BANDWIDTH_CONSTRAINT;


    protected void init() {
        if (!isNullOrEmpty(k)) {
            maxpaths = Integer.valueOf(k);
        }
        if (!isNullOrEmpty(plconstraint)) {
            packetlossConstraint = Double.parseDouble(plconstraint);
            filter = true;
        }
        if (!isNullOrEmpty(latconstraint)) {
            latencyConstraint = Double.parseDouble(latconstraint);
            filter = true;
        }
        if (!isNullOrEmpty(bwconstraint)) {
            bandwidthConstraint = Double.parseDouble(bwconstraint);
            filter = true;
        }
        pathService = get(PathService.class);
        hostService = get(HostService.class);
        topologyService = get(TopologyService.class);
    }
    @Override
    protected void execute() {
        init();

        HostId src = HostId.hostId(srcArg);
        HostId dst = HostId.hostId(dstArg);
        if (isNullOrEmpty(src.toString()) || isNullOrEmpty(dst.toString())) {
            print("The src or dst host is null!");
        }
        Host srchost = hostService.getHost(src);
        Host dsthost = hostService.getHost(dst);
        DefaultTopology.setDefaultMaxPaths(maxpaths);
        DefaultTopology.setDefaultGraphPathSearch(KSP);
        MMwaveLinkWeight mMwaveLinkWeight = new MMwaveLinkWeight();
        HopLinkWeight hopLinkWeight = new HopLinkWeight();
        Set<Path> paths;
        if (mmwave) {
             paths = pathService.getPaths(src, dst, mMwaveLinkWeight);
        } else {
            paths = pathService.getPaths(src, dst, hopLinkWeight);
        }
        if (paths.isEmpty()) {
            print("The path is empty!");
            return;
        }
        Link srclink = getEdgeLink(srchost, true);
        Link dstlink = getEdgeLink(dsthost, false);
        List<Path> result = new ArrayList<>();
        List<Double> resultMaxband = new ArrayList<>();
        List<Double> resultPl = new ArrayList<>();
        List<Double> resultLat = new ArrayList<>();
        List<Path> finalResult = new ArrayList<>();
        List<Double> finalResultMaxband = new ArrayList<>();


        Iterator<Path> it = paths.iterator();
        while (it.hasNext()) {
            double minBand = INFINITY;
            Path potentialPath = it.next();
            int count = 0;
            List<Link> links = potentialPath.links();
            List<Link> pathlinks = new ArrayList<>();
            for (int i = 1; i < links.size() - 1; i++) {
                 pathlinks.add(links.get(i));
            }
            for (Link link : pathlinks) {
                String band = link.annotations().value("bandwidth");
                if (!isNullOrEmpty(band)) {
                    bandwidth = Double.parseDouble(band);
                } else {
                    bandwidth = 0;
                }
                if (bandwidth < bandwidthConstraint) {
                    break;
                } else if (bandwidth < minBand) {
                    minBand = bandwidth;
                    count = count + 1;
                } else {
                    count = count + 1;
                }
            }
            if (count == pathlinks.size()) {
                result.add(potentialPath);
                resultMaxband.add(minBand);
            }
        }
        for (int i = 0; i < result.size(); i++) {
            List<Link> pathlinks = result.get(i).links();
            for (Link pathlink : pathlinks) {
                String len = pathlink.annotations().value("length");
                if (!isNullOrEmpty(len)) {
                    double ps = Psuccess.getPs(Double.parseDouble(len));
                    totalPs = totalPs * ps;
                }
                String lat = pathlink.annotations().value("mmlatency");
                if (!isNullOrEmpty(lat)) {
                    double latency = Double.parseDouble(lat);
                    totalLatency = totalLatency + latency;
                }
            }
            totalLoss = 1 - totalPs;
            // If the path's loss is higher than the constraint, continue.
            // Otherwise put it to the result.
            if (totalLoss < packetlossConstraint
                    && totalLatency < latencyConstraint) {
                finalResult.add(result.get(i));
                finalResultMaxband.add(resultMaxband.get(i));
                resultPl.add(totalLoss);
                resultLat.add(totalLatency);
                totalPs = 1.0;
                totalLatency = 0.0;
            } else {
                totalPs = 1.0;
                totalLatency = 0.0;
            }
        }
            if (outputJson()) {
                print("%s", json(this, result));
            } else if (filter) {
                 if (!finalResult.isEmpty()) {
                     print("There are %s paths which satify the requirements", finalResult.size());
                     for (int i = 0; i < finalResult.size(); i++) {
                         String loss = String.valueOf((int) (resultPl.get(i) * 100)) + "%";
                         String plConstraint = String.valueOf(packetlossConstraint * 100) + "%";
                         String latency = String.valueOf((resultLat.get(i))) + "ms";
                         String latConstraint = String.valueOf(latencyConstraint) + "ms";
                         print("The total packet loss is %s below %s", loss, plConstraint);
                         print("The total latency is %s below %s ", latency, latConstraint);
                         print("The bandwidth of each link in the path is greater than %s ",
                                 String.valueOf(bandwidthConstraint) + "Mbps");
                         print("The minimum bandwidth which is greater than bandwidth constraint in the path is %s ",
                                 String.valueOf(finalResultMaxband.get(i)) + "Mbps");
                         print(pathString(finalResult.get(i), srclink, dstlink));
                     }
                    } else {
                        print("No path meets the requirements!");
                    }
            } else {
                for (Path path : paths) {
                    print(pathString(path, srclink, dstlink));
                }
                print("There are %s available paths, the maximum of paths is %s", paths.size(), maxpaths);
            }
        }

    /**
     * Produces a JSON array containing the specified paths.
     *
     * @param context context to use for looking up codecs
     * @param paths collection of paths
     * @return JSON array
     */
    private static JsonNode json(AbstractShellCommand context, Iterable<Path> paths) {
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
    private  String pathString(Path path, Link srclink, Link dstlink) {
        StringBuilder sb = new StringBuilder();
        List<Link> links = path.links();

        sb.append(compactaSrcEdgeLinkString(links.get(0))).append(SEP);
        for (int i = 1; i < links.size() - 1; i++) {
            sb.append(compactLinkString(links.get(i))).append(SEP);
        }
        sb.append(compactaDstEdgeLinkString(links.get(links.size() - 1)));
        Weight cost = path.weight();
        sb.append("; cost=").append(((ScalarWeight) cost).value());
        return sb.toString();
    }
    /**
      *  LinkWeight on each Edge.
     **/
    class MMwaveLinkWeight implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge edge) {

           return getWeight(edge.link());
        }

        @Override
        public Weight getInitialWeight() {
            return INITIAL_WEIGHT;
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
     *  Hop counts on each Edge.
     **/
    class HopLinkWeight implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge edge) {
            Short v = edge.link().state() ==
                    ACTIVE ? (edge.link().type() ==
                    INDIRECT ? Short.MAX_VALUE : 1) : -1;
            return new ScalarWeight(v);

        }

        @Override
        public Weight getInitialWeight() {
            return INITIAL_WEIGHT;
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
     * @param host the host to use
     * @param isIngress whether it is Ingress to Device or not.
     * @return the connected Edgelink
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
        return path.edges().stream().map(TopologyEdge::link)
                .collect(Collectors.toList());
    }
    // Converts graph path to a network path with the same cost.
    private static org.onosproject.net.Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link)
                .collect(Collectors.toList());
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }
    private static Weight getWeight(Link link) {
        String v = link.annotations().value("length");
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
}
