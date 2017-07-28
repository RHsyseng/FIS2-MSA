package com.redhat.refarch.ecom.repository

import com.redhat.refarch.ecom.model.Order
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/***
 * @author jary@redhat.com
 */
interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerId(String customerId)

    List<Order> findByStatus(Order.Status status)

    Order getById(String id)
}
