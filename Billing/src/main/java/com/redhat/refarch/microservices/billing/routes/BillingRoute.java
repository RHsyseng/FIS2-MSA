/**
 *  Copyright 2005-2017 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.billing.routes;

import com.redhat.refarch.microservices.billing.model.Result;
import com.redhat.refarch.microservices.billing.model.Transaction;
import com.redhat.refarch.microservices.billing.service.BillingService;
import org.apache.camel.LoggingLevel;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/***
 * @author jary@redhat.com
 */
@Component
public class BillingRoute extends SpringRouteBuilder {

    @Autowired
    private BillingService billingService;

    @Autowired
    private DataFormatFactory dataFormatFactory;

    @Override
    public void configure() throws Exception {

        /*
            It's possible to bypass this route declaration completely by adding an annotation on the bean methods
            which will receive a message. Example:

                public class BillingService {

                    @Consume(uri = "amq:billing.orders.new")
                    public Result process(String transactionJson) { ... }

            However, notice that the payload received is now a JSON string. TypeConversion will still be required, so
             in the name of separation of concerns, I chose to keep type conversions in a defined route. Also refer to
             @Produce if interested in publishing directly to queues from beans rather than through a route.
         */

        from("amq:billing.orders.new")
                .routeId("processNewOrders")
                .unmarshal(dataFormatFactory.formatter(Transaction.class))
                .bean(billingService, "process")
                .marshal(dataFormatFactory.formatter(Result.class));

        from("amq:billing.orders.refund")
                .routeId("processRefunds")
                .unmarshal(dataFormatFactory.formatter(Transaction.class))
                .bean(billingService, "refund")
                .marshal(dataFormatFactory.formatter(Result.class));
    }
}