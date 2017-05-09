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
package com.redhat.refarch.microservices.trigger.service;

import com.redhat.refarch.microservices.trigger.model.Customer;
import com.redhat.refarch.microservices.trigger.model.Product;
import com.redhat.refarch.microservices.trigger.model.Result;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class TriggerService {

    private Logger logger = Logger.getLogger(getClass().getName());

    public JSONObject doPurchase() throws Exception {

        HttpClient client = new DefaultHttpClient();

        //get a customer
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "bobdole");
        jsonObject.put("password", "password");
        URIBuilder uriBuilder = getUriBuilder("customers", "authenticate");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got login response " + responseString);
        JSONObject jsonResponse = new JSONObject(responseString);
        Customer customer = new Customer();
        customer.setId(jsonResponse.getLong("id"));
        customer.setAddress(jsonResponse.getString("address"));
        customer.setName(jsonResponse.getString("name"));

        //initialize an order
        jsonObject = new JSONObject()
            .put("status", "Initial");
        uriBuilder = getUriBuilder("customers", customer.getId(), "orders");

        post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        response = client.execute(post);

        responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
        jsonResponse = new JSONObject(responseString);
        Long orderId = jsonResponse.getLong("id");


        // get an item
        uriBuilder = getUriBuilder("products");
        uriBuilder.addParameter("featured", "");

        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        response = client.execute(get);
        responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
        JSONArray jsonArray = new JSONArray(responseString);
        List<Map<String, Object>> products = Utils.getList(jsonArray);
        logInfo("array info " + Arrays.toString(products.toArray()));
        Map<String, Object> item = products.get(0);

        // put item on order
        jsonObject = new JSONObject()
            .put("sku", item.get("sku"))
            .put("quantity", 1);
        uriBuilder = getUriBuilder("customers", customer.getId(), "orders", orderId, "orderItems");
        post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        response = client.execute(post);
        responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);

        // billing/process
        jsonObject = new JSONObject()
                .put("amount", item.get("price"))
                .put("creditCardNumber", 1234567890123456L)
                .put("expMonth", 1)
                .put("expYear", 2019)
                .put("verificationCode", 123)
                .put("billingAddress", customer.getAddress())
                .put("customerName", customer.getName())
                .put("customerId", customer.getId())
                .put("orderNumber", orderId);

        logInfo(jsonObject.toString());

        uriBuilder = getUriBuilder("billing", "process");
        post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));

        logInfo("Executing " + post);
        response = new DefaultHttpClient().execute(post);
        responseString = EntityUtils.toString(response.getEntity());

        logInfo("Transaction processed as: " + responseString);
        return new JSONObject(responseString);
    }

    private static URIBuilder getUriBuilder(Object... path) {

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http");
        uriBuilder.setHost("gateway-service");
        uriBuilder.setPort(9091);

        StringWriter stringWriter = new StringWriter();
        for (Object part : path) {
            stringWriter.append('/').append(String.valueOf(part));
        }
        uriBuilder.setPath(stringWriter.toString());
        return uriBuilder;
    }

    private void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}