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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.math3.special.Erf;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Path;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.MMWaveIntent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final int ETHERNET_DEFAULT_COST = 101;
    /**
     * Default weight based on ETHERNET default weight.
     */
    public static final ScalarWeight ETHERNET_DEFAULT_WEIGHT =
            new ScalarWeight(ETHERNET_DEFAULT_COST);

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
        return new DefaultPath(path.providerId(), reverseLinks, path.weight());
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
    class MMwaveLinkWeight implements LinkWeigher {

        @Override
        public Weight weight(TopologyEdge edge) {

            //AnnotationKeys
            //This can help us to define cost function by annotations
            String v = edge.link().annotations().value("length");


            try {

                if (v != null) {
                    double ps = getPs(Double.parseDouble(v));
                    return  new ScalarWeight(1 + 1 / ps);
                } else {
                    return ETHERNET_DEFAULT_WEIGHT;
                }
                //total cost = fixed cost + dynamic cost
                // In Ethernet case, total cost = 100 + 1; (ps = 1)
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
    private  double getPs(double d) {
        double pl = 0.0;
        double alpha = 0.0;  //dB
        double beta = 0.0;
        double sigma = 0.0;   //dB
        double sigmaLin = 0.0;
        double xsiLin = 0.0;
        double xsi = 0.0;
        double fc;
        String plModel = "28";
        if (plModel.equals("28")) {
            alpha = 72.0;
            beta = 2.92;
            sigma = 8.7;
            sigmaLin = Math.pow(10, sigma / 10);
            xsiLin = Math.pow(sigmaLin, 2);
            xsi = 10 * Math.log10(xsiLin);
            pl = alpha + 10 * beta * Math.log10(d);
        } else if (plModel.equals("73")) {
            alpha = 86.6;
            beta = 2.45;
            sigma = 8.0;
            sigmaLin = Math.pow(10, sigma / 10);
            xsiLin = Math.pow(sigmaLin, 2);
            xsi = 10 * Math.log10(xsiLin);
            pl = alpha + 10 * beta * Math.log10(d);

        } else if (plModel.equals("3gpp")) {
            fc = 2.5;
            pl = 22.7 + 36.7 * Math.log10(d) + 26 * Math.log10(fc);
            beta = 3.67;
            alpha = 70.0;
        } else {
            System.out.println("Please type the right pl_model");
        }

        double lamda = 100.0; //density of mm-wave links
        double b = 2e9;         //mm-wave bandwidth
        double c = 0.11;      //fractional Los area in the model developed


        double gmax = 18.0;  //dB
        double gmaxLin = Math.pow(10, gmax / 10);

        double pb = 30.0;   //dB
        double pbLin = Math.pow(10, pb / 10);

        double pn = -174.0 + 10 * Math.log10(b) + 10;
        double pnLin = Math.pow(10, pn / 10);

        double plLin = Math.pow(10, pl / 10);

        double snr0 = pb + gmax - pn;
        double snr0Lin = Math.pow(10, snr0 / 10);

        double snr = snr0 - pl;
        double snrLin = Math.pow(10, snr / 10);
        double xsiLLin = 5.2;        //xsi corresponding path-loss standard deviation
        double xsiNLin = 7.6;

        //beta_l_n = one meter loss #dB
        double betaLN = alpha; //db
        double betaLNLin = Math.pow(10, betaLN / 10);

        //ml_db = -0.1 *beta_l_lin *log(10)
        double ml = -Math.log(betaLNLin);
        double sigmaL = 0.1 * xsiLLin * Math.log(10);

        //mn_db = -0.1*beta_n_lin * log(10)
        double mn = -Math.log(betaLNLin);
        double sigmaN = 0.1 * xsiNLin * Math.log(10);

        double tau = 3.0;
        double tauLin = Math.pow(10, tau / 10);


        //((Pb_lin*Gmax_lin)/(pl_lin*pn_lin)
        double factor = snrLin / tauLin;

        double pc1Q1 = qfun((Math.log((Math.pow(d, beta) / factor)) - ml) / sigmaL);
        double pc1Q2 = qfun((Math.log((Math.pow(d, pl) / factor)) - mn) / sigmaN);
        double pc1 = Math.pow(d, 2) * (pc1Q1 - pc1Q2);
        double pc2 = pcC1(sigmaL, ml, beta, factor) * pcC2(sigmaL, ml, d, beta, factor);
        double pc3 = pcC1(sigmaN, mn, beta, factor) * (1 / c - pcC2(sigmaN, mn, d, beta, factor));
        double pc = pc1 + pc2 + pc3;
        double lamdaA = lamda * Math.PI * c * pc;
        double ma = lamdaA / lamda;
        double ps = 1 - Math.exp(-lamda * ma * factor);
        return ps;
    }

    private  double qfun(double x) {
        return 0.5 * Erf.erfc(x / Math.sqrt(2));
    }

    private  double pcC1(double sigma, double m, double beta, double factor) {
        return Math.pow(factor, 2 / beta) * Math.exp(2 * ((sigma * sigma) / (beta * beta)) + 2 * (m / beta));
    }

    private   double pcC2(double sigma, double m, double d, double beta, double factor) {
        return qfun((sigma * sigma * (2 / beta) - Math.log(Math.pow(d, beta / factor)) + m) / sigma);

    }




}






