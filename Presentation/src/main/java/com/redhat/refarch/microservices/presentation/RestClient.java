package com.redhat.refarch.microservices.presentation;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestClient {

    public static void setProductsAttribute(HttpServletRequest request) {
        try {
            List<Map<String, Object>> products;
            String query = request.getParameter("query");
            if (query == null || query.isEmpty()) {
                products = getFeaturedProducts();
            } else {
                products = searchProducts(query);
            }
            request.setAttribute("products", products);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Failed to retrieve products: " + e.getMessage());
        }
    }

    private static List<Map<String, Object>> searchProducts(String query) throws IOException, JSONException, URISyntaxException, HttpErrorException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products");
        for (String keyword : query.split("\\s+")) {
            uriBuilder.addParameter("keyword", keyword);
        }
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        if (isError(response)) {
            throw new HttpErrorException(response);
        } else {
            String responseString = EntityUtils.toString(response.getEntity());
            JSONArray jsonArray = new JSONArray(responseString);
            List<Map<String, Object>> products = Utils.getList(jsonArray);
            return products;
        }
    }

    public static List<Map<String, Object>> getFeaturedProducts() throws IOException, JSONException, URISyntaxException, HttpErrorException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products");
        uriBuilder.addParameter("featured", "");
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        if (isError(response)) {
            throw new HttpErrorException(response);
        } else {
            String responseString = EntityUtils.toString(response.getEntity());
            JSONArray jsonArray = new JSONArray(responseString);
            List<Map<String, Object>> products = Utils.getList(jsonArray);
            return products;
        }
    }

    public static void register(HttpServletRequest request) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        String[] customerAttributes = new String[]{"name", "address", "telephone", "email", "username", "password"};
        JSONObject jsonObject = Utils.getJsonObject(request, customerAttributes);
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("customers");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        if (isError(response)) {
            request.setAttribute("errorMessage", "Failed to register customer");
        } else {
            String responseString = EntityUtils.toString(response.getEntity());
            logInfo("Got " + responseString);
            jsonObject.put("id", new JSONObject(responseString).getLong("id"));
            request.getSession().setAttribute("customer", Utils.getCustomer(jsonObject));
            request.getSession().setAttribute("itemCount", 0);
            getPendingOrder(request, jsonObject.getLong("id"));
        }
    }

    public static void login(HttpServletRequest request) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = Utils.getJsonObject(request, "username", "password");
        URIBuilder uriBuilder = getUriBuilder("customers", "authenticate");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        if (isError(response)) {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                request.setAttribute("errorMessage", "Incorrect password");
            } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                request.setAttribute("errorMessage", "Customer not found");
                request.setAttribute("username", request.getParameter("username"));
            } else {
                request.setAttribute("errorMessage", "Failed to login");
            }
        } else {
            String responseString = EntityUtils.toString(response.getEntity());
            logInfo("Got login response " + responseString);
            JSONObject jsonResponse = new JSONObject(responseString);
            request.getSession().setAttribute("customer", Utils.getCustomer(jsonResponse));
            request.getSession().setAttribute("itemCount", 0);
            getPendingOrder(request, jsonResponse.getLong("id"));
        }
    }

    private static void getPendingOrder(HttpServletRequest request, long customerId) throws ClientProtocolException, IOException, JSONException,
            URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders");
        uriBuilder.addParameter("status", "Initial");
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        if (isError(response) == false) {
            String responseString = EntityUtils.toString(response.getEntity());
            logInfo("Got " + responseString);
            JSONArray orderArray = new JSONArray(responseString);
            if (orderArray.length() == 0) {
                request.getSession().removeAttribute("orderId");
                request.getSession().removeAttribute("orderItems");
                request.getSession().setAttribute("itemCount", 0);
                request.removeAttribute("cart");
            } else {
                JSONObject orderJson = orderArray.getJSONObject(0);
                request.getSession().setAttribute("orderId", orderJson.getLong("id"));
                JSONArray jsonArray = orderJson.getJSONArray("orderItems");
                List<OrderItem> orderItems = new ArrayList<OrderItem>();
                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject orderItemJson = jsonArray.getJSONObject(index);
                    OrderItem orderItem = new OrderItem();
                    orderItem.setSku(orderItemJson.getLong("sku"));
                    orderItem.setId(orderItemJson.getLong("id"));
                    orderItem.setQuantity(orderItemJson.getInt("quantity"));
                    populateProductInfo(orderItem);
                    orderItems.add(orderItem);
                }
                request.getSession().setAttribute("orderItems", orderItems);
                int cartSize = 0;
                for (OrderItem orderItem : orderItems) {
                    cartSize += orderItem.getQuantity();
                }
                request.getSession().setAttribute("itemCount", cartSize);
                if (cartSize == 0) {
                    request.removeAttribute("cart");
                }
            }
        }
    }

    private static void populateProductInfo(OrderItem orderItem) throws ClientProtocolException, IOException, JSONException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products", orderItem.getSku());
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
        JSONObject jsonResponse = new JSONObject(responseString);
        orderItem.setAvailability(jsonResponse.getInt("availability"));
        orderItem.setDescription(jsonResponse.getString("description"));
        orderItem.setFeatured(jsonResponse.getBoolean("featured"));
        orderItem.setHeight(jsonResponse.getInt("height"));
        orderItem.setImage(jsonResponse.getString("image"));
        orderItem.setLength(jsonResponse.getInt("length"));
        orderItem.setName(jsonResponse.getString("name"));
        orderItem.setPrice(jsonResponse.getDouble("price"));
        orderItem.setWeight(jsonResponse.getInt("weight"));
        orderItem.setWidth(jsonResponse.getInt("width"));
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Enumeration<String> attrNames = session.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            session.removeAttribute(attrNames.nextElement());
        }
    }

    public static void purchase(HttpServletRequest request) throws ClientProtocolException, IOException, JSONException, URISyntaxException {
        long sku = Long.valueOf(request.getParameter("sku"));
        int availability = getProductAvailability(sku);
        if (availability == 0) {
            request.setAttribute("errorMessage", "The selected item is not available for purchase!");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) request.getSession().getAttribute("customer");
        long customerId = (Long) customer.get("id");
        Long orderId = (Long) request.getSession().getAttribute("orderId");
        if (orderId == null) {
            orderId = addInitialOrder(customerId);
            addOrderItem(customerId, orderId, sku, 1);
        } else {
            @SuppressWarnings("unchecked")
            List<OrderItem> orderItems = (List<OrderItem>) request.getSession().getAttribute("orderItems");
            OrderItem orderItem = null;
            for (OrderItem thisOrderItem : orderItems) {
                if (thisOrderItem.getSku() == sku) {
                    orderItem = thisOrderItem;
                    break;
                }
            }
            if (orderItem == null) {
                addOrderItem(customerId, orderId, sku, 1);
            } else {
                long orderItemId = orderItem.getId();
                int quantity = orderItem.getQuantity() + 1;
                updateOrderItem(request, customerId, orderId, orderItemId, sku, quantity);
            }
        }
        getPendingOrder(request, customerId);
    }

    private static int getProductAvailability(long sku) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products", sku);
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject jsonResponse = new JSONObject(responseString);
        return jsonResponse.getInt("availability");
    }

    private static long addInitialOrder(long customerId) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Initial");
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
        JSONObject jsonResponse = new JSONObject(responseString);
        return jsonResponse.getLong("id");
    }

    private static long addOrderItem(long customerId, long orderId, long sku, int quantity) throws JSONException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sku", sku);
        jsonObject.put("quantity", quantity);
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders", orderId, "orderItems");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
        JSONObject jsonResponse = new JSONObject(responseString);
        return jsonResponse.getLong("id");
    }

    private static void updateOrderItem(HttpServletRequest request, long customerId, long orderId, long orderItemId, Long sku, int quantity)
            throws JSONException, IOException, URISyntaxException {
        if (sku == null) {
            sku = getOrderedProductSku(customerId, orderId, orderItemId);
        }
        int availability = getProductAvailability(sku);
        if (quantity > availability) {
            quantity = availability;
            request.setAttribute("errorMessage", "Requested quantity exceeds product availability");
        }
        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("quantity", quantity);
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders", orderId, "orderItems", orderItemId);
        HttpPatch patch = new HttpPatch(uriBuilder.build());
        patch.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + patch);
        HttpResponse response = client.execute(patch);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
    }

    private static Long getOrderedProductSku(long customerId, long orderId, long orderItemId) throws JSONException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders", orderId, "orderItems", orderItemId);
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject jsonResponse = new JSONObject(responseString);
        return jsonResponse.getLong("sku");
    }

    private static void deleteOrderItem(long customerId, long orderId, long orderItemId) throws JSONException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders", orderId, "orderItems", orderItemId);
        HttpDelete delete = new HttpDelete(uriBuilder.build());
        logInfo("Executing " + delete);
        HttpResponse response = client.execute(delete);
        logInfo("Got response " + response.getStatusLine());
    }

    public static void updateQuantity(HttpServletRequest request) throws ClientProtocolException, IOException, JSONException, URISyntaxException {
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) request.getSession().getAttribute("customer");
        long customerId = (Long) customer.get("id");
        Long orderId = (Long) request.getSession().getAttribute("orderId");
        Long orderItemId = Long.valueOf(request.getParameter("orderItemId"));
        int quantity = Integer.valueOf(request.getParameter("quantity"));
        if (quantity == 0) {
            deleteOrderItem(customerId, orderId, orderItemId);
        } else {
            updateOrderItem(request, customerId, orderId, orderItemId, null, quantity);
        }
        getPendingOrder(request, customerId);
    }

    public static void completeOrder(HttpServletRequest request) throws ClientProtocolException, IOException, JSONException, URISyntaxException {
        JSONObject jsonResponse = processTransaction(request);
        String status = jsonResponse.getString("status");
        if ("SUCCESS".equals(status)) {
            @SuppressWarnings("unchecked")
            List<OrderItem> orderItems = (List<OrderItem>) request.getSession().getAttribute("orderItems");
            try {
                HttpResponse response = reduceInventory(orderItems);
                if (isError(response)) {
                    throw new HttpErrorException(response);
                }
            } catch (Exception e) {
                refundTransaction(jsonResponse.getInt("transactionNumber"));
                request.setAttribute("errorMessage", "Insufficient inventory to fulfill order");
                return;
            }
            try {
                markOrderPayment(request, jsonResponse);
                request.setAttribute("successMessage", "Your order has been processed");
            } catch (Exception e) {
                logInfo("Order " + request.getSession().getAttribute("orderId") + " processed but not updated in the database");
                request.setAttribute("errorMessage", "Order processed. Allow some time for update!");
            }
            request.getSession().removeAttribute("orderId");
            request.getSession().removeAttribute("orderItems");
            request.getSession().setAttribute("itemCount", 0);
        } else if ("FAILURE".equals(status)) {
            request.setAttribute("errorMessage", "Your credit card was declined");
        }
    }

    private static JSONObject processTransaction(HttpServletRequest request) throws IOException, JSONException, URISyntaxException {

        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) request.getSession().getAttribute("customer");

        JSONObject jsonObject = new JSONObject()
                .put("amount", Double.valueOf(request.getParameter("amount")))
                .put("creditCardNumber", Long.valueOf(request.getParameter("creditCardNo")))
                .put("expMonth", Integer.valueOf(request.getParameter("expirationMM")))
                .put("expYear", Integer.valueOf(request.getParameter("expirationYY")))
                .put("verificationCode", Integer.valueOf(request.getParameter("verificationCode")))
                .put("billingAddress", customer.get("address"))
                .put("customerName", customer.get("name"))
                .put("customerId", customer.get("id"))
                .put("orderNumber", request.getSession().getAttribute("orderId"));

        logInfo(jsonObject.toString());

        URIBuilder uriBuilder = getUriBuilder("billing", "process");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));

        logInfo("Executing " + post);
        HttpResponse response = new DefaultHttpClient().execute(post);
        String responseString = EntityUtils.toString(response.getEntity());

        logInfo("Transaction processed as: " + responseString);
        return new JSONObject(responseString);
    }

    private static void refundTransaction(int transactionNumber) throws URISyntaxException, IOException {

        URIBuilder uriBuilder = getUriBuilder("billing", "refund", transactionNumber);
        HttpPost post = new HttpPost(uriBuilder.build());

        logInfo("Executing " + post);
        HttpResponse response = new DefaultHttpClient().execute(post);

        logInfo("Transaction refund response code: " + response.getStatusLine());
    }

    private static HttpResponse reduceInventory(List<OrderItem> orderItems) throws URISyntaxException, IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            Map<String, Object> map = new HashMap<>();
            map.put("sku", orderItem.getSku());
            map.put("quantity", orderItem.getQuantity());
            list.add(map);
        }
        JSONArray jsonArray = new JSONArray(list);
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products", "reduce");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonArray.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        return response;
    }

    private static void markOrderPayment(HttpServletRequest request, JSONObject jsonResponse) throws JSONException, URISyntaxException, IOException {
        Long transactionNumber = jsonResponse.getLong("transactionNumber");
        Long transactionDate = jsonResponse.getLong("transactionDate");
        Long orderId = jsonResponse.getLong("orderNumber");
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) request.getSession().getAttribute("customer");
        Long customerId = (Long) customer.get("id");

        HttpClient client = new DefaultHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Paid");
        jsonObject.put("transactionNumber", transactionNumber);
        jsonObject.put("transactionDate", transactionDate);

        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders", orderId);
        HttpPatch patch = new HttpPatch(uriBuilder.build());
        patch.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + patch);
        HttpResponse response = client.execute(patch);
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got response " + responseString);
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

    public static void getOrderHistory(HttpServletRequest request) throws URISyntaxException, ParseException, IOException, JSONException {
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) request.getSession().getAttribute("customer");
        long customerId = (Long) customer.get("id");
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("customers", customerId, "orders");
        HttpGet get = new HttpGet(uriBuilder.build());
        logInfo("Executing " + get);
        HttpResponse response = client.execute(get);
        if (isError(response) == false) {
            String responseString = EntityUtils.toString(response.getEntity());
            logInfo("Got " + responseString);
            JSONArray orderArray = new JSONArray(responseString);
            List<Order> orders = new ArrayList<Order>();
            for (int index = 0; index < orderArray.length(); index++) {
                JSONObject orderJson = orderArray.getJSONObject(index);
                Order order = new Order();
                order.setId(orderJson.getLong("id"));
                order.setStatus(orderJson.getString("status"));
                if (!orderJson.isNull("transactionNumber")) {
                    order.setTransactionNumber(orderJson.getLong("transactionNumber"));
                }
                if (!orderJson.isNull("transactionDate")) {
                    order.setTransactionDate(new Date(orderJson.getLong("transactionDate")));
                }
                JSONArray orderItemArray = orderJson.getJSONArray("orderItems");
                for (int itemIndex = 0; itemIndex < orderItemArray.length(); itemIndex++) {
                    JSONObject orderItemJson = orderItemArray.getJSONObject(itemIndex);
                    OrderItem orderItem = new OrderItem();
                    orderItem.setSku(orderItemJson.getLong("sku"));
                    orderItem.setId(orderItemJson.getLong("id"));
                    orderItem.setQuantity(orderItemJson.getInt("quantity"));
                    populateProductInfo(orderItem);
                    order.addOrderItem(orderItem);
                }
                orders.add(order);
            }
            Collections.sort(orders, reverseOrderNumberComparator);
            request.setAttribute("orders", orders);
        }
    }

    private static boolean isError(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() >= HttpStatus.SC_BAD_REQUEST) {
            return true;
        } else {
            return false;
        }
    }

    private static void logInfo(String message) {
        Logger.getLogger(RestClient.class.getName()).log(Level.INFO, message);
    }

    public static Long addProduct(JSONObject jsonObject) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        if (isError(response)) {
            logInfo("Failed to add product: " + EntityUtils.toString(response.getEntity()));
            return null;
        } else {
            String responseString = EntityUtils.toString(response.getEntity());
            logInfo("Got " + responseString);
            return new JSONObject(responseString).getLong("sku");
        }
    }

    public static void addKeyword(JSONObject jsonObject) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products", "keywords");
        HttpPost post = new HttpPost(uriBuilder.build());
        post.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        if (isError(response)) {
            logInfo("Failed to add keyword");
        }
        String responseString = EntityUtils.toString(response.getEntity());
        logInfo("Got " + responseString);
    }

    public static void classifyProduct(long sku, List<String> keywords) throws JSONException, ClientProtocolException, IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder uriBuilder = getUriBuilder("products", "classify", sku);
        HttpPost post = new HttpPost(uriBuilder.build());
        JSONArray jsonArray = new JSONArray();
        for (String keyword : keywords) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("keyword", keyword);
            jsonArray.put(jsonObject);
        }
        post.setEntity(new StringEntity(jsonArray.toString(), ContentType.APPLICATION_JSON));
        logInfo("Executing " + post);
        HttpResponse response = client.execute(post);
        if (isError(response)) {
            logInfo("Failed to add keyword: " + EntityUtils.toString(response.getEntity()));
        } else {
            logInfo("Got " + response.getStatusLine());
        }
    }

    private static Comparator<Order> reverseOrderNumberComparator = new Comparator<Order>() {

        @Override
        public int compare(Order order1, Order order2) {
            return (int) (order2.getId() - order1.getId());
        }
    };
}