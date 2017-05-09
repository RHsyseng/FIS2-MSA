/*
 * Copyright 2005-2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.gateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.net.URL;

/***
 * @author jary@redhat.com
 */
@Component
public class GatewayUriProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        String url = exchange.getIn().getHeader("CamelHttpUri").toString();
        if(!url.startsWith("http") && !url.startsWith("https"))
            url = "http://" + url;

        url = new URL(url).getPath();
        exchange.getIn().setHeader("uriPath", url);
        String outPattern;

        if(url.startsWith("/product"))
            outPattern = "product-service";

        else if(url.startsWith("/billing"))
            outPattern = "billing-service";

        else if(url.startsWith("/customers"))
            outPattern = "sales-service";

        else
            throw new Exception("unknown context received on API Gateway");

        exchange.getIn().setHeader("newHostContext", outPattern);
    }
}
