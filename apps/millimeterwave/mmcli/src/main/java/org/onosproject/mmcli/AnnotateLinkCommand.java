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
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;

import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Link;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;


@Command(scope = "onos", name = "annotate-links",
        description = "Annotates links")
public class AnnotateLinkCommand extends AbstractShellCommand {


    @Option(name = "--both",
            description = "Add to both direction")
    private boolean both = false;
    static final ProviderId PID = new ProviderId("cli", "org.onosproject.cli", true);

    @Argument(index = 0, name = "srcArg", description = "source connection point",
            required = true, multiValued = false)
    String srcArg = null;


    @Argument(index = 1, name = "dstArg", description = "destination connection point",
            required = true, multiValued = false)
    String dstArg = null;


    @Argument(index = 2, name = "key", description = "Annotation key",
            required = true, multiValued = false)
    String key = null;

    @Argument(index = 3, name = "value",
            description = "Annotation value (null to remove)",
            required = false, multiValued = false)
    String value = null;



    @Override
    protected void execute() {

        LinkProviderRegistry registry = get(LinkProviderRegistry.class);
        LinkProvider provider = new AnnotationProvider();
        ConnectPoint src = ConnectPoint.deviceConnectPoint(srcArg);
        ConnectPoint dst = ConnectPoint.deviceConnectPoint(dstArg);


        try {
            LinkProviderService providerService = registry.register(provider);
            providerService.linkDetected(description(src, dst, key, value));
            if (both) {
                providerService.linkDetected(description(dst, src,
                        key, value));
            }
        } finally {
            registry.unregister(provider);
        }
    }

    private LinkDescription description(ConnectPoint src, ConnectPoint dst, String keyValue, String valueofkey) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();

        if (valueofkey != null) {
            builder.set(keyValue, valueofkey);
        } else {
            builder.remove(keyValue);
        }

        //Use source and destination connection port to define the specific link
        LinkService service = get(LinkService.class);
        Link link = service.getLink(src, dst);
        return new DefaultLinkDescription(src, dst, link.type(), builder.build());

    }


    // Token provider entity
    private static final class AnnotationProvider extends AbstractProvider implements LinkProvider {
        private AnnotationProvider() {
            super(PID);
        }


    }





}




