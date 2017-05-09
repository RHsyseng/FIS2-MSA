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
package com.redhat.refarch.microservices.gateway.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/***
 *
 * the main microservice API Gateway, taking all rest calls on any context and routing them as needed
 *
 * @author jary@redhat.com
 */
@Component
public class GatewayRoute extends SpringRouteBuilder {

    private GatewayUriProcessor uriProcessor;

    @Autowired
    public GatewayRoute(GatewayUriProcessor uriProcessor) {
        this.uriProcessor = uriProcessor;
    }

    @Override
    public void configure() throws Exception {

        String restletGatewayUri = "restlet:http://0.0.0.0:9091/{endpoint}";
        String newHostContext = "http4://${headers.newHostContext}:8080${headers.uriPath}";

        // error handler for all following routes
        errorHandler(defaultErrorHandler()
                .allowRedeliveryWhileStopping(false)
                .maximumRedeliveries(3)
                .redeliveryDelay(3000)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // establish restlet on 9091 to intercept ALL contexts/rest requests for forwarding to underlying microservices
        from(restletGatewayUri + "?restletMethods=post,get,put,patch,delete&restletUriPatterns=#uriTemplates")
                .routeId("proxy-api-gateway")
                .log(LoggingLevel.INFO, "[${exchangeId}] request rec'd in API Gateway entry point: ")
                .to("log:INFO?showBody=true&showHeaders=true")

                // GatewayUriProcessor grabs the context and path and helps do a first-step content-based routing
                .process(uriProcessor)
                .log(LoggingLevel.INFO, "[${exchangeId}] post URI processing: ")
                .to("log:INFO?showBody=true&showHeaders=true")
                .choice()

                    // we want all billing to go through amq for messaging backing
                    .when(simple("${headers.newHostContext} =~ 'billing-service'"))
                        .log(LoggingLevel.INFO, "[${exchangeId}] URI processed, billing-service request found: ")
                        .to("log:INFO?showBody=true&showHeaders=true")
                        .to("direct:billingRoute")

                    // product and sales calls can just mirror through directly to their respect rest API via http
                    .otherwise()
                        .log(LoggingLevel.INFO, "[${exchangeId}] URI processed, proxy request found: ")
                        .to("log:INFO?showBody=true&showHeaders=true")
                        .recipientList(simple(newHostContext + "?bridgeEndpoint=true"))
                .end();

        // calls to billing are Request/Reply (InOut) via active-mq for fault tolerance
        from("direct:billingRoute")
                .routeId("billingMsgGateway")
                .log(LoggingLevel.INFO, "[${exchangeId}] entering billingMsgGateway: ")
                .to("log:INFO?showBody=true&showHeaders=true")
                .choice()
                    .when(header("uriPath").startsWith("/billing/process"))
                        .log(LoggingLevel.INFO, "[${exchangeId}] billing/process request, sending to amq: ")
                        .to("log:INFO?showBody=true&showHeaders=true")
                        .to("amq:billing.orders.new?transferException=true&jmsMessageType=Text")
                        .log(LoggingLevel.INFO, "[${exchangeId}] also wiretapping to warehouse")
                        .wireTap("direct:warehouse")
                    .endChoice()

                    .when(header("uriPath").startsWith("/billing/refund"))
                        .log(LoggingLevel.INFO, "[${exchangeId}] billing/refund request, sending to amq: ")
                        .to("log:INFO?showBody=true&showHeaders=true")
                        .to("amq:billing.orders.refund?transferException=true&jmsMessageType=Text")

                    .otherwise()
                        .log(LoggingLevel.ERROR, "unknown method received in billingMsgGateway")
                .end();

        // calls to warehouse are used as Event Messages (InOnly) via active-mq for fault tolerance
        from("direct:warehouse")
                .routeId("warehouseMsgGateway")
                .log(LoggingLevel.INFO, "[${exchangeId}] entering warehouse: ")
                .to("log:INFO?showBody=true&showHeaders=true")

                // filter out transactions that failed or faulted out so we don't fulfill
                .filter(simple("${bodyAs(String)} contains 'SUCCESS'"))
                    .log(LoggingLevel.INFO, "[${exchangeId}] FILTERED SUCCESS, FOWARDING TO WAREHOUSES")
                    .to("log:INFO?showBody=true&showHeaders=true")
                    .inOnly("amq:topic:warehouse.orders?jmsMessageType=Text");
    }
}