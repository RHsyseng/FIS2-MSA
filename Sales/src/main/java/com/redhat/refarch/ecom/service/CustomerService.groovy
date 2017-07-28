package com.redhat.refarch.ecom.service

import com.redhat.refarch.ecom.model.Customer
import com.redhat.refarch.ecom.model.Order
import com.redhat.refarch.ecom.model.Order.Status
import com.redhat.refarch.ecom.model.OrderItem
import com.redhat.refarch.ecom.repository.CustomerRepository
import com.redhat.refarch.ecom.repository.OrderItemRepository
import com.redhat.refarch.ecom.repository.OrderRepository
import org.apache.camel.Consume
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

@Component
class CustomerService {

    @Autowired
    CustomerRepository customerRepository

    @Autowired
    OrderRepository orderRepository

    @Autowired
    OrderItemRepository orderItemRepository

    Customer getCustomer(String customerId) {
        return customerRepository.findOne(customerId)
    }

    Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer)
    }

    void deleteCustomer(String customerId) {
        customerRepository.delete(customerId)
    }
    
    Customer authenticate(Customer customer) {

        Customer result = customerRepository.getByUsername(customer.getUsername())
        if (result.getPassword() != customer.getPassword()) {
            throw new WebApplicationException(HttpURLConnection.HTTP_UNAUTHORIZED)
        }
        return result
    }

    Order getOrder(String orderId) {
        return orderRepository.getById(orderId)
    }

    List<Order> listOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId)
    }

    Order saveOrder(String customerId, Order order) {
        order.customerId = customerId
        return orderRepository.save(order)
    }

    Response deleteOrder(String orderId) {
        orderRepository.delete(orderId)
        return Response.ok().build()
    }

    OrderItem getOrderItem(String orderItemId) {
        return orderItemRepository.getById(orderItemId)
    }

    List<OrderItem> listOrderItems(String orderId) {

        List<String> orderItemIds = getOrder(orderId).getOrderItemIds()
        return orderItemIds.isEmpty() ? [] : orderItemRepository.findByIdIn(orderItemIds)
    }
    
    OrderItem saveOrderItem(String orderId, OrderItem orderItem) {

        OrderItem result = orderItemRepository.save(orderItem)
        Order order = orderRepository.getById(orderId)
        if (!order.getOrderItemIds().contains(result.getId()))
            order.getOrderItemIds().add(result.getId())
        orderRepository.save(order)
        return result
    }

    Response deleteOrderItem(String orderItemId) {
        orderItemRepository.delete(orderItemId)
        return Response.ok().build()
    }
}