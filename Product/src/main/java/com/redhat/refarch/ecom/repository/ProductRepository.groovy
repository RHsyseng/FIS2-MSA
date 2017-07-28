package com.redhat.refarch.ecom.repository

import com.redhat.refarch.ecom.model.Product
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/***
 * @author jary@redhat.com
 */
interface ProductRepository extends MongoRepository<Product, String> {

    Product getBySku(String sku)

    List<Product> findByKeywords(String keyword)

    List<Product> findByIsFeatured(Boolean isFeatured)
}
