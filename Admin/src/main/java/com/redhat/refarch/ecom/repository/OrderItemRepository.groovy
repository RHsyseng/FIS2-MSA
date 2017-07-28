package com.redhat.refarch.ecom.repository

import com.redhat.refarch.ecom.model.OrderItem
import org.springframework.data.mongodb.repository.MongoRepository

/***
 * @author jary@redhat.com
 */
interface OrderItemRepository extends MongoRepository<OrderItem, String> {

    List<OrderItem> findByIdIn(List<String> orderItemIds)

    OrderItem getById(String id)
}
