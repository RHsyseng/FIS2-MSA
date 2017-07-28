package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id

@EqualsAndHashCode
@ApiModel(description = "Represents the quantity of each product requested in an order")
class OrderItem {

    @Id
    @ApiModelProperty(value = "Persistence ID", required = false)
    String id

    @ApiModelProperty(value = "Product SKU", required = true)
    String sku

    @ApiModelProperty(value = "Product quantity", required = true)
    Integer quantity
}