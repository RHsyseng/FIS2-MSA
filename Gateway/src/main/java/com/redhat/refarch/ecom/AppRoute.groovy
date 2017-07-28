/**
 * Copyright 2005-2017 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License") you may not use this file except in compliance
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
package com.redhat.refarch.ecom

import com.redhat.refarch.ecom.model.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.model.rest.RestParamType
import org.apache.camel.spring.SpringRouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.logging.Logger

@Component
class AppRoute extends SpringRouteBuilder {

    @Value('${gateway.token.header:X-3scale-proxy-secret-token}')
    String tokenHeader

    @Value('${gateway.token.value:#{null}}')
    String tokenValue

    Logger logger = Logger.getLogger(AppRoute.class.name)

    @Override
    void configure() throws Exception {

        restConfiguration().component("spark-rest")
                .host("0.0.0.0")
                .port(9091)
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "E-Commerce API Gateway")
                .apiProperty("api.version", "1.0")
                .apiProperty("api.description", "Serving downstream product, sales, billing and fulfillment services")
                .apiProperty("cors", "true")
                .apiProperty("api.license", "MIT License (MIT)")
                .apiProperty("api.licenseUrl", "https://opensource.org/licenses/MIT")

        if (tokenValue != null && tokenHeader != null) {

            logger.info("AUTH TOKEN REQUIREMENT DETECTED: [${tokenHeader}] adding security route interceptor")
            interceptFrom().id("auth token interceptor").process(new Processor() {
                @Override
                void process(Exchange exchange) throws Exception {

                    if (exchange.in.getHeader(tokenHeader) == null
                            || exchange.in.getHeader(tokenHeader) != tokenValue) {

                        logger.info("Authorization token required, but header missing or invalid")
                        exchange.out.setHeader(Exchange.HTTP_RESPONSE_CODE, 403)
                        exchange.out.setBody("Unauthorized [missing or invalid token]")
                        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE)
                    }
                }
            })
        }

        rest("/billing/process").description("billing processing & warehouse fulfillment")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .post()
                    .description("process transaction").type(Transaction.class).outType(Result.class)
                    .param().name("body").type(RestParamType.body)
                    .description("transaction to be processed").endParam()
                    .responseMessage().code(200).message("billing complete, forking to fulfillment")
                    .endResponseMessage()
                    .route()
                    .to("amq:billing.process?transferException=true")
                    .wireTap("direct:warehouse")

        from("direct:warehouse")
                .routeId("warehouseMsgGateway")
                .filter(simple('${bodyAs(String)} contains "SUCCESS"'))
                .inOnly("amq:topic:warehouse.fulfill")

        rest("/billing/refund/{transactionNumber}").description("billing refunds endpoint")
                .consumes(MediaType.APPLICATION_JSON)
                .get()
                    .description("process refund")
                    .param().name("transactionNumber").type(RestParamType.path)
                    .description("transactionNumber to be refunded").endParam()
                    .responseMessage().code(200).message("billing refund complete").endResponseMessage()
                    .to("amq:billing.refund?transferException=true")

        rest("/customers").description("customers endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .post()
                    .description("save new customer").type(Customer.class).outType(Customer.class)
                    .param().name("body").type(RestParamType.body)
                    .description("customer to save").endParam()
                    .responseMessage().code(200).message("new customer saved").endResponseMessage()
                    .to("amq:customers.save?transferException=true")
                .put()
                    .description("update customer").type(Customer.class).outType(Customer.class)
                    .param().name("body").type(RestParamType.body)
                    .description("customer to update").endParam()
                    .responseMessage().code(200).message("customer updated").endResponseMessage()
                    .to("amq:customers.save?transferException=true")

                .patch()
                    .description("partial update customer").type(Customer.class).outType(Customer.class)
                    .param().name("body").type(RestParamType.body)
                    .description("customer to update").endParam()
                    .responseMessage().code(200).message("customer updated").endResponseMessage()
                    .to("amq:customers.save?transferException=true")

        rest("/customers/authenticate").description("customer authentication endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .post()
                    .description("authenticate customer").type(Customer.class).outType(Customer.class)
                    .param().name("body").type(RestParamType.body)
                    .description("customer to authenticate").endParam()
                    .responseMessage().code(200).message("customer authenticated").endResponseMessage()
                    .to("amq:customers.authenticate?transferException=true")

        rest("/customers/{customerId}").description("individual customer endpoint")
                .get()
                    .produces(MediaType.APPLICATION_JSON)
                    .description("get customer").outType(Customer.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer to fetch").endParam()
                    .responseMessage().code(200).message("customer fetched").endResponseMessage()
                    .to("amq:customers.get?transferException=true")

                .delete()
                    .description("delete customer")
                    .param().name("customerId").type(RestParamType.path)
                    .description("customer to delete").endParam()
                    .responseMessage().code(200).message("customer deleted").endResponseMessage()
                    .to("amq:customers.delete?transferException=true")

        rest("/customers/{customerId}/orders").description("orders endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("get customer's orders").outTypeList(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer to fetch orders from").endParam()
                    .responseMessage().code(200).message("customer's orders fetched").endResponseMessage()
                    .to("amq:customers.orders.list?transferException=true")
                .post()
                    .description("save new customer order").type(Order.class).outType(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer to own order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("order to save").endParam()
                    .responseMessage().code(200).message("new customer order saved").endResponseMessage()
                    .to("amq:customers.orders.save?transferException=true")
                .put()
                    .description("update customer order").type(Order.class).outType(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer to own order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("order to update").endParam()
                    .responseMessage().code(200).message("customer order updated").endResponseMessage()
                    .to("amq:customers.orders.save?transferException=true")

                .patch()
                    .description("partial update customer order").type(Order.class).outType(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("order to update").endParam()
                    .responseMessage().code(200).message("customer order updated").endResponseMessage()
                    .to("amq:customers.orders.save?transferException=true")

        rest("/customers/{customerId}/orders/{orderId}").description("individual order endpoint")
                .get()
                    .produces(MediaType.APPLICATION_JSON)
                    .description("get customer order").outType(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order to fetch").endParam()
                    .responseMessage().code(200).message("customer order fetched").endResponseMessage()
                    .to("amq:customers.orders.get?transferException=true")

                .delete()
                    .description("delete customer order").outType(Response.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("order to delete").endParam()
                    .responseMessage().code(200).message("customer order deleted").endResponseMessage()
                    .to("amq:customers.orders.delete?transferException=true")

        rest("/customers/{customerId}/orders/{orderId}/orderItems").description("order items endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("get order items").outType(Order.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .responseMessage().code(200).message("order items fetched").endResponseMessage()
                    .to("amq:customers.orders.orderItems.list?transferException=true")
                .post()
                    .description("save new order item").type(OrderItem.class).outTypeList(OrderItem.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("orderItem to save").endParam()
                    .responseMessage().code(200).message("new order item saved").endResponseMessage()
                    .to("amq:customers.orders.orderItems.save?transferException=true")
                .put()
                    .description("update order item").type(OrderItem.class).outTypeList(OrderItem.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("orderItem to update").endParam()
                    .responseMessage().code(200).message("order item updated").endResponseMessage()
                    .to("amq:customers.orders.orderItems.save?transferException=true")
                .patch()
                    .description("partial update order item").type(OrderItem.class).outTypeList(OrderItem.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("orderItem to update").endParam()
                    .responseMessage().code(200).message("order item updated").endResponseMessage()
                    .to("amq:customers.orders.orderItems.save?transferException=true")

        rest("/customers/{customerId}/orders/{orderId}/orderItems/{orderItemId}")
                .description("individual order item endpoint")
                .get()
                    .produces(MediaType.APPLICATION_JSON)
                    .description("get order item").outType(OrderItem.class)
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .param().name("orderItemId").type(RestParamType.path)
                    .description("Id of orderItem to fetch").endParam()
                    .responseMessage().code(200).message("order item fetched").endResponseMessage()
                    .to("amq:customers.orders.orderItems.get?transferException=true")

                .delete()
                    .description("delete order item")
                    .param().name("customerId").type(RestParamType.path)
                    .description("id of customer owning order").endParam()
                    .param().name("orderId").type(RestParamType.path)
                    .description("id of order").endParam()
                    .param().name("orderItemId").type(RestParamType.path)
                    .description("id of orderItem to delete").endParam()
                    .responseMessage().code(200).message("order item deleted").endResponseMessage()
                    .to("amq:customers.orders.orderItems.delete?transferException=true")

        rest("/products").description("products endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("list products").outTypeList(Product.class)
                    .responseMessage().code(200).message("products fetched").endResponseMessage()
                    .to("amq:products.list.all?transferException=true")
                .post()
                    .description("save new product").type(Product.class).outType(Product.class)
                    .param().name("body").type(RestParamType.body)
                    .description("product to save").endParam()
                    .responseMessage().code(200).message("new product saved").endResponseMessage()
                    .to("amq:products.save?transferException=true")
                .put()
                    .description("update product").type(Product.class).outType(Product.class)
                    .param().name("body").type(RestParamType.body)
                    .description("product to update").endParam()
                    .responseMessage().code(200).message("product updated").endResponseMessage()
                    .to("amq:products.save?transferException=true")
                .patch()
                    .description("partial update product").type(Product.class).outType(Product.class)
                    .param().name("body").type(RestParamType.body)
                    .description("product to update").endParam()
                    .responseMessage().code(200).message("product updated").endResponseMessage()
                    .to("amq:products.save?transferException=true")

        rest("/products/featured").description("featured products endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("list featured products").outTypeList(Product.class)
                    .responseMessage().code(200).message("featured products fetched").endResponseMessage()
                    .to("amq:products.list.featured?transferException=true")

        rest("/products/{sku}").description("individual product endpoint")
                .consumes(MediaType.TEXT_PLAIN).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("get product").outType(Product.class)
                    .param().name("sku").type(RestParamType.path)
                    .description("sku of product to fetch").endParam()
                    .responseMessage().code(200).message("product fetched").endResponseMessage()
                    .to("amq:products.get?transferException=true")
                .delete()
                    .description("delete product")
                    .param().name("sku").type(RestParamType.path)
                    .description("product to delete").endParam()
                    .responseMessage().code(200).message("product deleted").endResponseMessage()
                    .to("amq:products.delete?transferException=true")

        rest("/products/reduction")
                .consumes(MediaType.APPLICATION_JSON)
                .post()
                    .description("reduce product inventory").typeList(OrderItem.class)
                    .param().name("body").type(RestParamType.body)
                    .description("orderItems to reduce").endParam()
                    .responseMessage().code(200).message("product inventory reduced").endResponseMessage()
                    .to("amq:products.reduce?transferException=true")

        rest("/products/{sku}/keywords").description("product keywords endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .post()
                    .description("add keywords to product").typeList(String.class).outType(Product.class)
                    .param().name("sku").type(RestParamType.path)
                    .description("sku product to add keywords to").endParam()
                    .param().name("body").type(RestParamType.body)
                    .description("collection of keywords to add").endParam()
                    .responseMessage().code(200).message("keywords added to product").endResponseMessage()
                    .to("amq:products.keywords.add?transferException=true")

        rest("/products/keywords/{keyword}").description("keyword lookup endpoint")
                .consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON)
                .get()
                    .description("get products by keywords").outTypeList(Product.class)
                    .param().name("keyword").type(RestParamType.path)
                    .description("keyword to fetch products by").endParam()
                    .responseMessage().code(200).message("products with keyword fetched").endResponseMessage()
                    .to("amq:products.list.keyword?transferException=true")

        rest("/demo/reset")
                .get()
                    .description("reset demo dataset - removes all customers/orders, builds basic product set")
                    .responseMessage().code(200).message("demo dataset reset").endResponseMessage()
                    .to("amq:admin.reset?transferException=true")

        rest("/demo/testApi")
                .get()
                    .description("test API endpoints")
                    .responseMessage().code(200).message("API test successful").endResponseMessage()
                    .to("amq:admin.testApi?transferException=true")
    }
}