/**
 * Copyright 2017 ZTE Corporation.
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

package org.onap.holmes.rulemgt;

import io.dropwizard.setup.Environment;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.onap.holmes.common.config.MicroServiceConfig;
import org.onap.holmes.common.dropwizard.ioc.bundle.IOCApplication;
import org.onap.holmes.common.exception.CorrelationException;
import org.onap.holmes.common.utils.MSBRegisterUtil;
import org.onap.holmes.common.utils.transactionid.TransactionIdFilter;
import org.onap.holmes.rulemgt.dcae.DcaeConfigurationPolling;
import org.onap.holmes.rulemgt.msb.MsbQuery;
import org.onap.holmes.rulemgt.resources.RuleMgtResources;
import org.onap.msb.sdk.discovery.entity.MicroServiceInfo;
import org.onap.msb.sdk.discovery.entity.Node;

@Slf4j
public class RuleActiveApp extends IOCApplication<RuleAppConfig> {

    public static void main(String[] args) throws Exception {
        new RuleActiveApp().run(args);
    }

    @Override
    public String getName() {
        return "Holmes Rule Management ActiveApp APP ";
    }

    @Override
    public void run(RuleAppConfig configuration, Environment environment) throws Exception {
        super.run(configuration, environment);

        environment.jersey().register(new RuleMgtResources());
        try {
            new MSBRegisterUtil().register2Msb(createMicroServiceInfo());
        } catch (CorrelationException e) {
            log.warn(e.getMessage(), e);
        }

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(
                new DcaeConfigurationPolling(MicroServiceConfig.getEnv(MicroServiceConfig.HOSTNAME)), 0,
                DcaeConfigurationPolling.POLLING_PERIOD, TimeUnit.MILLISECONDS);
        environment.servlets().addFilter("customFilter",new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet
                .allOf(DispatcherType.class),true,"/*");

        new MsbQuery().startTimer();
    }

    private MicroServiceInfo createMicroServiceInfo() {
        String[] serviceAddrInfo = MicroServiceConfig.getMicroServiceIpAndPort();
        MicroServiceInfo msinfo = new MicroServiceInfo();
        msinfo.setServiceName("holmes-rule-mgmt");
        msinfo.setVersion("v1");
        msinfo.setUrl("/api/holmes-rule-mgmt/v1");
        msinfo.setProtocol("REST");
        msinfo.setVisualRange("0|1");
        msinfo.setEnable_ssl(true);
        Set<Node> nodes = new HashSet<>();
        Node node = new Node();
        node.setIp(serviceAddrInfo[0]);
        node.setPort(serviceAddrInfo[1]);
        nodes.add(node);
        msinfo.setNodes(nodes);
        return msinfo;
    }
}
