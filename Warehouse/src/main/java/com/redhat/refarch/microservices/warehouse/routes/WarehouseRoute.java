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
package com.redhat.refarch.microservices.warehouse.routes;

import com.redhat.refarch.microservices.warehouse.model.Result;
import com.redhat.refarch.microservices.warehouse.service.WarehouseService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/***
 * @author jary@redhat.com
 */
@Component
public class WarehouseRoute extends SpringRouteBuilder {

    @Value("${messaging.warehouse.id}")
    private String warehouseId;

    private WarehouseService warehouseService;

    private DataFormatFactory dataFormatFactory;


    @Autowired
    public WarehouseRoute(WarehouseService warehouseService, DataFormatFactory dataFormatFactory) {
        this.warehouseService = warehouseService;
        this.dataFormatFactory = dataFormatFactory;
    }

    @Override
    public void configure() throws Exception {

        from("amq:topic:warehouse.orders?clientId=" + warehouseId)
                .routeId("fulfillOrder")

                .unmarshal(dataFormatFactory.formatter(Result.class))

                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        /*
                            In production cases, multiple warehouse instances would be subscribed to the
                            warehouse.orders topic, so this processor could be used to referenced a shared data grid
                            clustered over all warehouse  instances. With proper geographical and inventory level
                            information, a decision could be made as to whether this specific instance is the optimal
                            warehouse to fulfill the request or not. Note that doing so would require a lock
                            mechanism in the shared cache if the choice algorithm could potentially allow duplicate
                            optimal choices.
                         */

                        // in this demo, only a single warehouse instance will be used, so just claim all messages and return them
                        exchange.getIn().setHeader("ownership", "true");
                    }
                })

                .filter(simple("${headers.ownership} == 'true'"))
                    .bean(warehouseService, "fulfillOrder");

    }
}