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
import org.springframework.stereotype.Component;

/***
 *
 * the main microservice API Gateway, taking all rest calls on any context and routing them as needed
 *
 * @author jary@redhat.com
 */
@Component
public class GatewayRoute extends SpringRouteBuilder {

    @Override
    public void configure() throws Exception {

        // error handler for all following routes
        errorHandler(defaultErrorHandler()
                .allowRedeliveryWhileStopping(false)
                .maximumRedeliveries(1)
                .redeliveryDelay(3000)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // using spark-rest for 'splat' URI wildcard support
        restConfiguration().component("spark-rest").host("0.0.0.0").port(9091);

        // Call to billing/process go to billing.orders.new messaging queue
        rest("/billing/process")
                .post()
                .route()
                    .to("amq:billing.orders.new?transferException=true&jmsMessageType=Text")
                    .wireTap("direct:warehouse");

        // Call to billing/process go to billing.orders.refund messaging queue
        rest("/billing/refund")
                .post()
                .route()
                .to("amq:billing.orders.refund?transferException=true&jmsMessageType=Text");

        // 'customers' calls proxied to sales-service
        String endpoint = "http4://sales-service?bridgeEndpoint=true";
        rest("/customers")
                .get().toD(endpoint)
                .post().toD(endpoint);

        endpoint = "http4://sales-service/${headers.splat[0]}?bridgeEndpoint=true";
        rest("/customers/*")
                .get().toD(endpoint)
                .post().toD(endpoint)
                .patch().toD(endpoint)
                .delete().toD(endpoint);

        // 'products' calls proxied to product-service
        endpoint = "http4://product-service?bridgeEndpoint=true";
        rest ("/products")
                .get().toD(endpoint)
                .post().toD(endpoint);

        endpoint = "http4://product-service/${headers.splat[0]}?bridgeEndpoint=true";
        rest ("/products/*")
                .get().toD(endpoint)
                .post().toD(endpoint);

        from("direct:warehouse")
                .routeId("warehouseMsgGateway")
                // filter failed transactions
                .filter(simple("${bodyAs(String)} contains 'SUCCESS'"))
                .inOnly("amq:topic:warehouse.orders?jmsMessageType=Text");
    }
}