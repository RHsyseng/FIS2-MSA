package com.redhat.refarch.ecom.service

import com.redhat.refarch.ecom.model.Error
import com.redhat.refarch.ecom.model.OrderItem
import com.redhat.refarch.ecom.model.Product
import com.redhat.refarch.ecom.repository.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ProductService {

    @Autowired
    ProductRepository productRepository

    Product getProduct(String sku) {
        return productRepository.getBySku(sku)
    }

    List<Product> getProductsByKeyword(String keyword) {
        return productRepository.findByKeywords(keyword)
    }

    List<Product> list() {
        return productRepository.findAll()
    }

    List<Product> findFeatured() {
        return productRepository.findByIsFeatured(true)
    }

    Product saveProduct(Product product) {
        return productRepository.save(product)
    }

    void deleteProduct(String sku) {
        productRepository.delete(sku)
    }

    void reduceInventory(OrderItem[] orderItems) {

        for (OrderItem orderItem : orderItems) {
            println "processing orderItem ${orderItem.toString()}"
            Product product = getProduct(orderItem.sku)

            if (product == null) {
                throw new Error(HttpURLConnection.HTTP_NOT_FOUND, "Product not found").asException()
            }
            if (orderItem.quantity > product.getAvailability()) {
                String message = "Insufficient availability for ${orderItem.sku}"
                throw new Error(HttpURLConnection.HTTP_CONFLICT, message).asException()
            } else {
                product.setAvailability(product.getAvailability() - orderItem.quantity)
                productRepository.save(product)
            }
        }
        println "done, returning"
    }

    Product addKeywordsToProduct(String sku, String[] keywords) {

        Product product = getProduct(sku)
        product.setKeywords(Arrays.asList(keywords))
        saveProduct(product)
    }
}