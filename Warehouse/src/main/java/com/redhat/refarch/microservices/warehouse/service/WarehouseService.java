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
package com.redhat.refarch.microservices.warehouse.service;

import com.redhat.refarch.microservices.warehouse.model.Result;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class WarehouseService {

    private Logger logger = Logger.getLogger(getClass().getName());

    public void fulfillOrder(Result result) throws Exception {

        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Shipped");

        URIBuilder uriBuilder = new URIBuilder("http://gateway-service:9091/customers/" + result.getCustomerId()
                + "/orders/" + result.getOrderNumber());
        HttpPatch patch = new HttpPatch(uriBuilder.build());
        patch.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + patch);
        HttpResponse response = client.execute(patch);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
    }

    private void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}