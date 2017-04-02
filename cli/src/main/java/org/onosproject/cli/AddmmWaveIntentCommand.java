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
package org.onosproject.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import  org.onosproject.net.intent.MMWaveIntent;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentService;

import java.util.List;


/**
 * Created by dingdamu on 2017/3/17.
 */
@Command(scope = "onos", name = "mmwave-add-intent",
        description = "Installs mm-wave intents")
public class AddmmWaveIntentCommand extends ConnectivityIntentCommand {
    @Argument(index = 0, name = "src_host", description = "One host ID",
            required = true, multiValued = false)
    String srcArg = null;

    @Argument(index = 1, name = "dst_host" +
            "]", description = "Another host ID",
            required = true, multiValued = false)
    String dstArg = null;



    @Override
    protected void execute() {

        IntentService service = get(IntentService.class);

        HostId src = HostId.hostId(srcArg);
        HostId dst = HostId.hostId(dstArg);

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();
        List<Constraint> constraints = buildConstraints();

        MMWaveIntent intent = MMWaveIntent.builder()
                .appId(appId())
                .key(key())
                .one(src)
                .two(dst)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraints)
                .priority(priority())
                .build();
        service.submit(intent);
        print("mm-wave intent submitted:\n%s", intent.toString());



    }



}
