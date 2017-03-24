package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.*;
import org.onosproject.net.*;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.onosproject.net.intent.MMWaveIntent;
import java.util.*;
import java.util.stream.Collectors;

import static org.onosproject.net.Link.Type.EDGE;
import static org.onosproject.net.flow.DefaultTrafficSelector.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by dingdamu on 2017/3/20.
 */
@Component(immediate = true)
public class MMWaveIntentCompiler implements IntentCompiler<MMWaveIntent> {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_ID_NOT_FOUND = "Didn't find device id in the link";


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Activate
    public void activate() {
        intentManager.registerCompiler(MMWaveIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(MMWaveIntent.class);
    }

    @Override
    public List<Intent> compile(MMWaveIntent intent, List<Intent> installable) {
        // If source and destination are the same, there are never any installables.
        if (Objects.equals(intent.one(), intent.two())) {
            return ImmutableList.of();
        }

        boolean isAsymmetric = intent.constraints().contains(new AsymmetricPathConstraint());
        Path pathOne = getPath(intent.one(), intent.two());
        Path pathTwo = isAsymmetric ?
                getPath(intent.two(), intent.one()) : invertPath(pathOne);

        Host one = hostService.getHost(intent.one());
        Host two = hostService.getHost(intent.two());

        return Arrays.asList(createLinkCollectionIntent(pathOne, one, two, intent),
                createLinkCollectionIntent(pathTwo, two, one, intent));
    }

    // Inverts the specified path. This makes an assumption that each link in
    // the path has a reverse link available. Under most circumstances, this
    // assumption will hold.
    private Path invertPath(Path path) {
        List<Link> reverseLinks = new ArrayList<>(path.links().size());
        for (Link link : path.links()) {
            reverseLinks.add(0, reverseLink(link));
        }
        return new DefaultPath(path.providerId(), reverseLinks, path.cost());
    }

    // Produces a reverse variant of the specified link.
    private Link reverseLink(Link link) {
        return DefaultLink.builder().providerId(link.providerId())
                .src(link.dst())
                .dst(link.src())
                .type(link.type())
                .state(link.state())
                .isExpected(link.isExpected())
                .build();
    }


    private FilteredConnectPoint getFilteredPointFromLink(Link link) {
        FilteredConnectPoint filteredConnectPoint;
        if (link.src().elementId() instanceof DeviceId) {
            filteredConnectPoint = new FilteredConnectPoint(link.src());
        } else if (link.dst().elementId() instanceof DeviceId) {
            filteredConnectPoint = new FilteredConnectPoint(link.dst());
        } else {
            throw new IntentCompilationException(DEVICE_ID_NOT_FOUND);
        }
        return filteredConnectPoint;
    }

    private Intent createLinkCollectionIntent(Path path,
                                              Host src,
                                              Host dst,
                                              MMWaveIntent intent) {
        /*
         * The path contains also the edge links, these are not necessary
         * for the LinkCollectionIntent.
         */
        Set<Link> coreLinks = path.links()
                .stream()
                .filter(link -> !link.type().equals(EDGE))
                .collect(Collectors.toSet());

        Link ingressLink = path.links().get(0);
        Link egressLink = path.links().get(path.links().size() - 1);

        FilteredConnectPoint ingressPoint = getFilteredPointFromLink(ingressLink);
        FilteredConnectPoint egressPoint = getFilteredPointFromLink(egressLink);

        TrafficSelector selector = builder(intent.selector())
                .matchEthSrc(src.mac())
                .matchEthDst(dst.mac())
                .build();

        return LinkCollectionIntent.builder()
                .key(intent.key())
                .appId(intent.appId())
                .selector(selector)
                .treatment(intent.treatment())
                .links(coreLinks)
                .filteredIngressPoints(ImmutableSet.of(
                        ingressPoint
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        egressPoint
                ))
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .priority(intent.priority())
                .build();
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     */
    protected Path getPath(ElementId one, ElementId two) {
        Set<Path> paths = pathService.getPaths(one, two, new MMwaveLinkWeight());

        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }


    class MMwaveLinkWeight implements LinkWeight {

        @Override
        public double weight(TopologyEdge edge) {

            //AnnotationKeys
            //This can help us to define cost function by annotations
            String v = edge.link().annotations().value("ps");
            try {
                return v != null ? 1 + (1 / (Double.parseDouble(v) / 100)) : 101;
                //total cost = fixed cost + dynamic cost
                // In Ethernet case, total cost = 100 + 1; (ps = 1)
                // In mm-wave case, total cost = 1 + 1/ps;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

    }

}







