package com.redhat.refarch.ecom.repository

import com.redhat.refarch.ecom.model.Customer
import org.springframework.data.mongodb.repository.MongoRepository

/***
 * @author jary@redhat.com
 */
interface CustomerRepository extends MongoRepository<Customer, String> {

    Customer getByUsername(String username)
}
