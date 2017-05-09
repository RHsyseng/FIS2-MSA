/**
 * Copyright 2005-2017 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.trigger.routes;

import com.redhat.refarch.microservices.trigger.service.TriggerService;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/***
 * @author jary@redhat.com
 */
@Component
public class TriggerRoute extends SpringRouteBuilder {

    private TriggerService triggerService;

    @Autowired
    public TriggerRoute(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @Override
    public void configure() throws Exception {

        restConfiguration().component("jetty")
                .bindingMode(RestBindingMode.auto)
                .dataFormatProperty("prettyPrint", "true")
                .port(9091);

        rest("/trigger")
                .get("/onePurchase").to("direct:onePurchase")
                .get("/doPurchases").to("direct:doPurchases");

        from("direct:onePurchase")
                .routeId("onePurchase")
                .bean(triggerService, "doPurchase");

        from("direct:doPurchases")
                .routeId("doPurchases")
                .loop(10).copy()
                .delay(3000)
                .bean(triggerService, "doPurchase");
    }
}