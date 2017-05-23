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

        String salesUri = "http4://sales-service:8080?bridgeEndpoint=true";
        String salesMethodUri = "http4://sales-service:8080/${headers.splat[0]}?bridgeEndpoint=true";
        String productUri = "http4://product-service:8080?bridgeEndpoint=true";
        String productMethodUri = "http4://product-service:8080/${headers.splat[0]}?bridgeEndpoint=true";

        errorHandler(defaultErrorHandler()
                .allowRedeliveryWhileStopping(false)
                .maximumRedeliveries(1)
                .redeliveryDelay(3000)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // using spark-rest for 'splat' URI wildcard support
        restConfiguration().component("spark-rest").host("0.0.0.0").port(9091);

        rest("/billing/process")
                .post()
                .route()
                    .to("amq:billing.orders.new?transferException=true&jmsMessageType=Text")
                    .wireTap("direct:warehouse");

        rest("/billing/refund")
                .post()
                .to("amq:billing.orders.refund?transferException=true&jmsMessageType=Text");

        rest("/customers")
                .get().toD(salesUri)
                .post().toD(salesUri);

        rest("/customers/*")
                .get().toD(salesMethodUri)
                .post().toD(salesMethodUri)
                .patch().toD(salesMethodUri)
                .delete().toD(salesMethodUri);

        rest ("/products")
                .get().toD(productUri)
                .post().toD(productUri);

        rest ("/products/*")
                .get().toD(productMethodUri)
                .post().toD(productMethodUri);

        from("direct:warehouse")
                .routeId("warehouseMsgGateway")
                .filter(simple("${bodyAs(String)} contains 'SUCCESS'"))
                .inOnly("amq:topic:warehouse.orders?jmsMessageType=Text");
    }
}