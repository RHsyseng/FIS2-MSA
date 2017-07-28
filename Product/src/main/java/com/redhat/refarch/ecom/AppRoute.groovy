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

import com.redhat.refarch.ecom.model.OrderItem
import com.redhat.refarch.ecom.model.Product
import com.redhat.refarch.ecom.service.ProductService
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.spring.SpringRouteBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AppRoute extends SpringRouteBuilder {

    @Autowired
    ProductService productService

    @Override
    void configure() throws Exception {

        from("amq:products.get")
                .bean(productService, 'getProduct(${header.sku})')
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.list.keyword")
                .bean(productService, 'getProductsByKeyword(${header.keyword})')
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.list.featured")
                .bean(productService, "findFeatured")
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.list.all")
                .bean(productService, "list")
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.save")
                .unmarshal().json(JsonLibrary.Jackson, Product.class)
                .bean(productService, "saveProduct")
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.delete")
                .bean(productService, 'deleteProduct(${header.sku})')

        from("amq:products.reduce")
                .unmarshal().json(JsonLibrary.Jackson, OrderItem[].class)
                .bean(productService, 'reduceInventory(${body})')
                .marshal().json(JsonLibrary.Jackson)

        from("amq:products.keywords.add")
                .unmarshal().json(JsonLibrary.Jackson, String[].class)
                .bean(productService, 'addKeywordsToProduct(${header.sku}, ${body})')
                .marshal().json(JsonLibrary.Jackson)
    }
}