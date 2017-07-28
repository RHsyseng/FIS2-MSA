/**
 *  Copyright 2005-2017 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License") you may not use this file except in compliance
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
package com.redhat.refarch.ecom.service

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.redhat.refarch.ecom.model.Customer
import com.redhat.refarch.ecom.model.Order
import com.redhat.refarch.ecom.model.OrderItem
import com.redhat.refarch.ecom.model.Product
import com.redhat.refarch.ecom.model.Result
import com.redhat.refarch.ecom.model.Transaction
import com.redhat.refarch.ecom.repository.CustomerRepository
import com.redhat.refarch.ecom.repository.OrderItemRepository
import com.redhat.refarch.ecom.repository.OrderRepository
import com.redhat.refarch.ecom.repository.ProductRepository
import org.apache.camel.Consume
import org.apache.http.HttpStatus
import org.apache.http.client.methods.*
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.hamcrest.collection.IsIterableContainingInAnyOrder
import org.hamcrest.collection.IsIterableContainingInOrder
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AdminService {

    @Autowired
    CustomerRepository customerRepository

    @Autowired
    ProductRepository productRepository

    @Autowired
    OrderRepository orderRepository

    @Autowired
    OrderItemRepository orderItemRepository

    @Value('${gateway.host:gateway-service}')
    String gatewayHost

    @Value('${gateway.port:9091}')
    Integer gatewayPort

    URIBuilder uriBuilder

    CloseableHttpClient httpClient

    Gson gson = new Gson()

    StringWriter stringWriter = new StringWriter()

    void resetData() {

        try {
            [customerRepository, productRepository, orderRepository, orderItemRepository].each { it.deleteAll() }

            JsonReader jsonReader = new JsonReader(
                    new InputStreamReader(AdminService.class.getResourceAsStream("/product_filler.json")))

            productRepository.save(Arrays.asList(new Gson().fromJson(jsonReader, Product[].class)))

        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    void testApi() {

        resetData()

        httpClient = HttpClients.createDefault()

        Customer customer = new Customer()
        customer.name = "Bob Dole"
        customer.address = "123 Somewhere St"
        customer.telephone = "1234567890"
        customer.email = "bob@dole.com"
        customer.username = "bobdole"
        customer.password = "password"

        Product newProduct = new Product.ProductBuilder()
                .name("Fancy TV")
                .description("Fancy television")
                .length(80.2)
                .width(1.5)
                .height(40.3)
                .weight(5.5)
                .featured(true)
                .availability(5)
                .price(99.99)
                .image("TV")
                .build()

        Order newOrder = new Order()
        newOrder.status = Order.Status.Initial
        newOrder.customerId = customer.id

        OrderItem newOrderItem = new OrderItem()
        newOrderItem.setQuantity(1)

        Transaction transaction = new Transaction()
        transaction.amount = 100.99
        transaction.creditCardNumber = 123457890123456
        transaction.expYear = 2030
        transaction.expMonth = 1

        // save new customer
        uri("customers")
        customer = (Customer) doPost(customer, Customer.class)
        Assert.assertNotNull(customer)
        Assert.assertNotNull(customer.id)
        Assert.assertEquals(customer, customerRepository.getByUsername("bobdole"))

        // get customer
        uri("customers", customer.id)
        customer = (Customer) doGet(Customer.class)
        Assert.assertNotNull(customer)
        Assert.assertEquals(customer, customerRepository.getByUsername("bobdole"))

        // authenticate customer
        uri("customers", "authenticate")
        customer = (Customer) doPost(customer, Customer.class)
        Assert.assertNotNull(customer)
        Assert.assertEquals(customer, customerRepository.getByUsername("bobdole"))

        // delete customer
        uri("customers", customer.getId())
        CloseableHttpResponse response = doDelete()
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertNull(customerRepository.getByUsername("bobdole"))

        // put customer
        uri("customers")
        customer = (Customer) doPut(customer, Customer.class)
        Assert.assertNotNull(customer)
        Assert.assertEquals(customer, customerRepository.getByUsername("bobdole"))

        // patch customer
        uri("customers")
        customer = (Customer) doPatch(customer, Customer.class)
        Assert.assertNotNull(customer)
        Assert.assertEquals(customer, customerRepository.getByUsername("bobdole"))

        // add product
        uri("products")
        Product product = (Product) doPost(newProduct, Product.class)
        Assert.assertNotNull(product)
        newProduct.setSku(product.sku)
        Assert.assertEquals(product, newProduct)

        // add keywords to product
        uri("products", product.sku, "keywords")
        response = doSilentPost(["Electronics", "TV"])
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        product = productRepository.findOne(product.sku)
        Assert.assertTrue(product.getKeywords().containsAll(["Electronics", "TV"]))

        // reduce product
        OrderItem item1 = new OrderItem()
        item1.id = null
        item1.sku = product.sku
        item1.quantity = 2
        uri("products", "reduction")
        response = doSilentPost([item1])
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertTrue(productRepository.findOne(product.sku).getAvailability() == 3)

        // delete product
        uri("products", product.sku)
        response = doDelete()
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertNull(productRepository.findOne(product.sku))

        // put product
        uri("products")
        product.setDescription("bar")
        product = (Product) doPut(product, Product.class)
        Assert.assertNotNull(product)
        Assert.assertTrue(product.description == "bar")

        // patch product
        product.setDescription("foo")
        uri("products")
        product = (Product) doPatch(product, Product.class)
        Assert.assertNotNull(product)
        Assert.assertTrue(product.description == "foo")

        // list products
        uri("products")
        List<Product> products = doGetList(Product[].class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertThat(products, IsIterableContainingInAnyOrder.containsInAnyOrder(
                productRepository.findAll().toArray()))

        // list featured products
        uri("products", "featured")
        products = doGetList(Product[].class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertThat(products, IsIterableContainingInAnyOrder.containsInAnyOrder(
                productRepository.findByIsFeatured(true).toArray()))

        // list products by keyword
        uri("products", "keywords", "Electronics")
        products = doGetList(Product[].class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertThat(products, IsIterableContainingInAnyOrder.containsInAnyOrder(
                productRepository.findByKeywords("Electronics").toArray()))

        // get product to check availability
        uri("products", product.getSku())
        product = (Product) doGet(Product.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertNotNull(products)
        Assert.assertEquals(product, productRepository.findOne(product.sku))
        Assert.assertEquals(product.availability, productRepository.findOne(product.sku).availability)

        // add order
        uri("customers", customer.id, "orders")
        Order order = (Order) doPost(newOrder, Order.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(order, orderRepository.findOne(order.id))

        // list orders
        uri("customers", customer.id, "orders")
        List<Order> orders = doGetList(Order[].class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertTrue(orders.contains(order))

        // delete order
        uri("customers", customer.id, "orders", order.id)
        response = doDelete()
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertNull(orderRepository.findOne(order.id))

        // put order
        order.setStatus(Order.Status.Paid)
        uri("customers", customer.id, "orders")
        order = (Order) doPut(order, Order.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(order, orderRepository.findOne(order.id))

        // patch order
        order.setStatus(Order.Status.InProgress)
        uri("customers", customer.id, "orders")
        order = (Order) doPatch(order, Order.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(order, orderRepository.findOne(order.id))

        // add order item
        newOrderItem.sku = product.sku
        uri("customers", customer.id, "orders", order.id, "orderItems")
        OrderItem orderItem = (OrderItem) doPost(newOrderItem, OrderItem.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(orderItem, orderItemRepository.findOne(orderItem.id))
        order = orderRepository.findOne(order.id)
        Assert.assertTrue(order.orderItemIds.size() == 1)
        Assert.assertTrue(order.orderItemIds.contains(orderItem.id))

        // put order item
        orderItem.quantity = 5
        uri("customers", customer.id, "orders", order.id, "orderItems")
        orderItem = (OrderItem) doPut(orderItem, OrderItem.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(orderItem, orderItemRepository.findOne(orderItem.id))
        Assert.assertTrue(orderItem.quantity == 5)
        order = orderRepository.findOne(order.id)
        Assert.assertTrue(order.orderItemIds.size() == 1)
        Assert.assertTrue(order.orderItemIds.contains(orderItem.id))

        // patch order item
        orderItem.quantity = 3
        uri("customers", customer.id, "orders", order.id, "orderItems")
        orderItem = (OrderItem) doPatch(orderItem, OrderItem.class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertEquals(orderItem, orderItemRepository.findOne(orderItem.id))
        Assert.assertTrue(orderItem.quantity == 3)
        order = orderRepository.findOne(order.id)
        Assert.assertTrue(order.orderItemIds.size() == 1)
        Assert.assertTrue(order.orderItemIds.contains(orderItem.id))

        // get order item
        uri("customers", customer.id, "orders", order.id, "orderItems", orderItem.id)
        orderItem = (OrderItem) doGet(OrderItem.class)
        Assert.assertNotNull(orderItem)
        Assert.assertEquals(orderItem, orderItemRepository.findOne(orderItem.id))

        // list order items
        uri("customers", customer.id, "orders", order.id, "orderItems")
        List<OrderItem> orderItems = doGetList(OrderItem[].class)
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertTrue(orderItems.contains(orderItem))

        // delete order item
        uri("customers", customer.id, "orders", order.id, "orderItems", orderItem.id)
        response = doDelete()
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        Assert.assertNull(orderItemRepository.findOne(orderItem.id))

        //test billing process
        transaction.customerId = customer.id
        transaction.orderNumber = order.id
        uri("billing", "process")
        Result result = (Result) doPost(transaction, Result.class)
        Assert.assertEquals(result.status, Result.Status.SUCCESS)
        transaction.setExpYear(2010)
        result = (Result) doPost(transaction, Result.class)
        Assert.assertEquals(result.status, Result.Status.FAILURE)

        //test billing refund
        uri("billing", "refund", "123")
        response = doSilentGet()
        Assert.assertTrue(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)

        resetData()
        println ">>>>> TESTS COMPLETE: API tests successful <<<<<"
    }

    private Object doPut(Object objToMarshal, Class clazz) {
        HttpPut put = new HttpPut(uriBuilder.build())
        put.setEntity(new StringEntity(gson.toJson(objToMarshal).toString(), ContentType.APPLICATION_JSON))
        return gson.fromJson(EntityUtils.toString(httpClient.execute(put).getEntity()), clazz)
    }

    private Object doGet(Class clazz) {
        HttpGet get = new HttpGet(uriBuilder.build())
        return gson.fromJson(EntityUtils.toString(httpClient.execute(get).getEntity()), clazz)
    }

    private List doGetList(Class clazz) {
        HttpGet get = new HttpGet(uriBuilder.build())
        return  Arrays.asList(gson.fromJson(EntityUtils.toString(httpClient.execute(get).getEntity()), clazz))
    }

    private CloseableHttpResponse doSilentGet() {
        HttpGet get = new HttpGet(uriBuilder.build())
        CloseableHttpResponse response = httpClient.execute(get)
        EntityUtils.consumeQuietly(response.getEntity())
        return response
    }

    private Object doPost(Object objToMarshal, Class clazz) {
        HttpPost post = new HttpPost(uriBuilder.build())
        post.setEntity(new StringEntity(gson.toJson(objToMarshal).toString(), ContentType.APPLICATION_JSON))
        return gson.fromJson(EntityUtils.toString(httpClient.execute(post).getEntity()), clazz)
    }

    private CloseableHttpResponse doSilentPost(Object objToMarshal) {
        HttpPost post = new HttpPost(uriBuilder.build())
        post.setEntity(new StringEntity(gson.toJson(objToMarshal).toString(), ContentType.APPLICATION_JSON))
        CloseableHttpResponse response = httpClient.execute(post)
        EntityUtils.consumeQuietly(response.getEntity())
        return response
    }

    private Object doPatch(Object objToMarshal, Class clazz) {
        HttpPatch patch = new HttpPatch(uriBuilder.build())
        patch.setEntity(new StringEntity(gson.toJson(objToMarshal).toString(), ContentType.APPLICATION_JSON))
        return gson.fromJson(EntityUtils.toString(httpClient.execute(patch).getEntity()), clazz)
    }

    private CloseableHttpResponse doDelete() {
        HttpDelete delete = new HttpDelete(uriBuilder.build())
        CloseableHttpResponse response = httpClient.execute(delete)
        EntityUtils.consumeQuietly(response.getEntity())
        return response
    }

    private void uri(Object... path) {

        if (uriBuilder == null) {
            uriBuilder = new URIBuilder()
                    .setScheme("http")
                    .setHost(gatewayHost)
                    .setPort(gatewayPort)
        }

        stringWriter.buffer.length = 0
        path.each { stringWriter.append("/${String.valueOf(it)}") }
        uriBuilder.setPath(stringWriter.toString())
    }
}